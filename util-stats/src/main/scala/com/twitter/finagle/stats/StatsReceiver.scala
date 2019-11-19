package com.twitter.finagle.stats

import java.lang.{Float => JFloat}
import java.util.concurrent.Callable
import scala.annotation.varargs

/**
 * Represent a verbosity level for a given metric.
 */
final class Verbosity private (override val toString: String)

object Verbosity {

  /**
   * Indicates that a given metric is for standard operations.
   */
  val Default: Verbosity = new Verbosity("Verbosity(default)")

  /**
   * Indicates that a given metric may only be useful for a debugging/troubleshooting purposes.
   */
  val Debug: Verbosity = new Verbosity("Verbosity(debug)")
}

object StatsReceiver {
  private[StatsReceiver] var immortalGauges: List[Gauge] = Nil
}

/**
 * [[StatsReceiver]] utility methods for ease of use from java.
 */
object StatsReceivers {

  /**
   * Java compatible version of [[StatsReceiver.addGauge]].
   */
  @varargs
  def addGauge(statsReceiver: StatsReceiver, callable: Callable[JFloat], name: String*): Gauge = {
    // scalafix:off StoreGaugesAsMemberVariables
    statsReceiver.addGauge(name: _*)(callable.call())
    // scalafix:on StoreGaugesAsMemberVariables
  }

  /**
   * Java compatible version of [[StatsReceiver.provideGauge]].
   */
  @varargs
  def provideGauge(statsReceiver: StatsReceiver, callable: Callable[JFloat], name: String*): Unit =
    statsReceiver.provideGauge(name: _*)(callable.call())
}

/**
 * An interface for recording metrics. Named [[Counter Counters]], [[Stat Stats]],
 * and [[Gauge Gauges]] can be accessed through the corresponding methods of this class.
 *
 * =Verbosity Levels=
 *
 * Each metric created via a stats receiver has a [[Verbosity verbosity level]] attached to it.
 * Distinguishing verbosity levels for metrics is optional and is up to a concrete implementation.
 * Doing this, however, helps to separate [[Verbosity.Debug debug metrics]] (only helpful in
 * troubleshooting) from their [[Verbosity.Default operationally-required counterparts]] (provide
 * a corresponding degree of visibility into a healthy process) thus potentially reducing the
 * observability cost.
 *
 * Metrics created w/o an explicitly specified [[Verbosity]] level, will use [[Verbosity.Default]].
 * Use [[VerbosityAdjustingStatsReceiver]] to adjust this behaviour.
 *
 * @see [[StatsReceivers]] for a Java-friendly API.
 */
trait StatsReceiver {

  /**
   * Specifies the representative receiver.  This is in order to
   * expose an object we can use for comparison so that global stats
   * are only reported once per receiver.
   */
  def repr: AnyRef

  /**
   * Accurately indicates if this is a [[NullStatsReceiver]].
   * Because equality is not forwarded via scala.Proxy, this
   * is helpful to check for a [[NullStatsReceiver]].
   */
  def isNull: Boolean = false

  /**
   * Get a [[MetricBuilder metricBuilder]] for this StatsReceiver.
   */
  def metricBuilder(): MetricBuilder = new MetricBuilder(statsReceiver = this)

  /**
   * Get a [[Counter counter]] with the given `name`.
   */
  @varargs
  def counter(name: String*): Counter = counter(Verbosity.Default, name: _*)

  /**
   * Get a [[Counter counter]] with the given `name`.
   */
  @varargs
  def counter(verbosity: Verbosity, name: String*): Counter

  /**
   * Get a [[Counter counter]] with the given schema.
   *
   * This default implementation passes the relevant fields to the pre-schema/pre-metadata interface.
   * StatsReceivers with support for schemas/metadata will override this to make better use of that
   * information.
   */
  private[stats] def counter(schema: CounterSchema): Counter =
    counter(schema.metricBuilder.verbosity, schema.metricBuilder.name: _*)

  /**
   * Get a [[Stat stat]] with the given name.
   */
  @varargs
  def stat(name: String*): Stat = stat(Verbosity.Default, name: _*)

  /**
   * Get a [[Stat stat]] with the given name.
   */
  @varargs
  def stat(verbosity: Verbosity, name: String*): Stat

  /**
   * Get a [[Stat stat]] with the given schema.
   *
   * This default implementation passes the relevant fields to the pre-schema/pre-metadata interface.
   * StatsReceivers with support for schemas/metadata will override this to make better use of that
   * information.
   */
  private[stats] def stat(schema: HistogramSchema): Stat =
    stat(schema.metricBuilder.verbosity, schema.metricBuilder.name: _*)

  /**
   * Register a function `f` as a [[Gauge gauge]] with the given name that has
   * a lifecycle with no end.
   *
   * This measurement exists in perpetuity.
   *
   * Measurements under the same name are added together.
   *
   * @see [[StatsReceiver.addGauge]] if you can properly control the lifecycle
   *     of the returned [[Gauge gauge]].
   */
  def provideGauge(name: String*)(f: => Float): Unit = {
    val gauge = addGauge(name: _*)(f)
    StatsReceiver.synchronized {
      StatsReceiver.immortalGauges ::= gauge
    }
  }

  /**
   * Add the function `f` as a [[Gauge gauge]] with the given name.
   *
   * The returned [[Gauge gauge]] value is only weakly referenced by the
   * [[StatsReceiver]], and if garbage collected will eventually cease to
   * be a part of this measurement: thus, it needs to be retained by the
   * caller. Or put another way, the measurement is only guaranteed to exist
   * as long as there exists a strong reference to the returned
   * [[Gauge gauge]] and typically should be stored in a member variable.
   *
   * Measurements under the same name are added together.
   *
   * @see [[StatsReceiver.provideGauge]] when there is not a good location
   *     to store the returned [[Gauge gauge]] that can give the desired lifecycle.
   *
   * @see [[https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html java.lang.ref.WeakReference]]
   */
  def addGauge(name: String*)(f: => Float): Gauge = addGauge(Verbosity.Default, name: _*)(f)

  /**
   * Add the function `f` as a [[Gauge gauge]] with the given name.
   *
   * The returned [[Gauge gauge]] value is only weakly referenced by the
   * [[StatsReceiver]], and if garbage collected will eventually cease to
   * be a part of this measurement: thus, it needs to be retained by the
   * caller. Or put another way, the measurement is only guaranteed to exist
   * as long as there exists a strong reference to the returned
   * [[Gauge gauge]] and typically should be stored in a member variable.
   *
   * Measurements under the same name are added together.
   *
   * @see [[StatsReceiver.provideGauge]] when there is not a good location
   *     to store the returned [[Gauge gauge]] that can give the desired lifecycle.
   *
   * @see [[https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html java.lang.ref.WeakReference]]
   */
  def addGauge(verbosity: Verbosity, name: String*)(f: => Float): Gauge

  /**
   * Add the function `f` as a [[Gauge gauge]] with the given name.
   *
   * The returned [[Gauge gauge]] value is only weakly referenced by the
   * [[StatsReceiver]], and if garbage collected will eventually cease to
   * be a part of this measurement: thus, it needs to be retained by the
   * caller. Or put another way, the measurement is only guaranteed to exist
   * as long as there exists a strong reference to the returned
   * [[Gauge gauge]] and typically should be stored in a member variable.
   *
   * Measurements under the same name are added together.
   *
   * This default implementation passes the relevant fields to the pre-schema/pre-metadata interface.
   * StatsReceivers with support for schemas/metadata will override this to make better use of that
   * information.
   *
   * @see [[StatsReceiver.provideGauge]] when there is not a good location
   *     to store the returned [[Gauge gauge]] that can give the desired lifecycle.
   * @see [[https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html java.lang.ref.WeakReference]]
   */
  private[stats] def addGauge(schema: GaugeSchema)(f: => Float): Gauge =
    addGauge(schema.metricBuilder.verbosity, schema.metricBuilder.name: _*)(f)

  /**
   * Prepend `namespace` to the names of the returned [[StatsReceiver]].
   *
   * For example:
   *
   * {{{
   *   statsReceiver.scope("client").counter("adds")
   *   statsReceiver.scope("client").scope("backend").counter("adds")
   * }}}
   *
   * will generate [[Counter counters]] named `/client/adds` and `/client/backend/adds`.
   *
   * Note it's recommended to be mindful with usage of the `scope` method as it's almost always
   * more efficient to pass a full metric name directly to a constructing method.
   *
   * Put this way, whenever possible prefer
   *
   * {{{
   *   statsReceiver.counter("client", "adds")
   * }}}
   *
   * to
   *
   * {{{
   *   statsReceiver.scope("client").counter("adds")
   * }}}
   */
  def scope(namespace: String): StatsReceiver = {
    if (namespace == "") this
    else {
      new NameTranslatingStatsReceiver(this, namespace) {
        protected def translate(name: Seq[String]): Seq[String] = namespace +: name
      }
    }
  }

  /**
   * Prepend `namespace` and `namespaces` to the names of the returned [[StatsReceiver]].
   *
   * For example:
   *
   * {{{
   *   statsReceiver.scope("client", "backend", "pool").counter("adds")
   * }}}
   *
   * will generate a [[Counter counter]] named `/client/backend/pool/adds`.
   *
   * Note it's recommended to be mindful with usage of the `scope` method as it's almost always
   * more efficient to pass a full metric name directly to a constructing method.
   *
   * Put this way, whenever possible prefer
   *
   * {{{
   *   statsReceiver.counter("client", "backend", "pool", "adds")
   * }}}
   *
   * to
   *
   * {{{
   *   statsReceiver.scope("client", "backend", "pool").counter("adds")
   * }}}
   */
  @varargs
  final def scope(namespaces: String*): StatsReceiver =
    namespaces.foldLeft(this)((statsReceiver, name) => statsReceiver.scope(name))

  /**
   * Prepend a suffix value to the next scope.
   *
   * For example:
   *
   * {{{
   *   statsReceiver.scopeSuffix("toto").scope("client").counter("adds")
   * }}}
   *
   * will generate a [[Counter counter]] named `/client/toto/adds`.
   */
  def scopeSuffix(suffix: String): StatsReceiver = {
    if (suffix == "") this
    else
      new StatsReceiverProxy {
        protected def self: StatsReceiver = StatsReceiver.this
        override def toString: String = s"$self/$suffix"
        override def scope(namespace: String): StatsReceiver = self.scope(namespace).scope(suffix)
      }
  }

  // These two are needed to accommodate Zookeper's circular dependency.
  // We'll remove them once zk is upgraded: CSL-4710.
  private[stats] def counter0(name: String): Counter = counter(name)
  private[stats] def stat0(name: String): Stat = stat(name)
}

abstract class AbstractStatsReceiver extends StatsReceiver {
  @varargs
  final override def counter(name: String*): Counter = counter(Verbosity.Default, name: _*)
  @varargs
  final def counter(verbosity: Verbosity, name: String*): Counter =
    counterImpl(verbosity, name)
  protected def counterImpl(verbosity: Verbosity, name: scala.collection.Seq[String]): Counter

  @varargs
  final override def stat(name: String*): Stat = stat(Verbosity.Default, name: _*)
  @varargs
  final def stat(verbosity: Verbosity, name: String*): Stat = statImpl(verbosity, name)
  protected def statImpl(verbosity: Verbosity, name: scala.collection.Seq[String]): Stat

  final override def addGauge(name: String*)(f: => Float): Gauge =
    addGauge(Verbosity.Default, name: _*)(f)
  final def addGauge(verbosity: Verbosity, name: String*)(f: => Float): Gauge =
    addGaugeImpl(verbosity, name, f)
  protected def addGaugeImpl(
    verbosity: Verbosity,
    name: scala.collection.Seq[String],
    f: => Float
  ): Gauge
}
