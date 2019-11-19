package com.twitter.finagle.stats

object NullStatsReceiver extends NullStatsReceiver {
  def get(): NullStatsReceiver.type = this
}

/**
 * A no-op [[StatsReceiver]]. Metrics are not recorded, making this receiver useful
 * in unit tests and as defaults in situations where metrics are not strictly
 * required.
 */
class NullStatsReceiver extends StatsReceiver {
  def repr: NullStatsReceiver = this

  private[this] val NullCounter = new Counter { def incr(delta: Long): Unit = () }
  private[this] val NullStat = new Stat { def add(value: Float): Unit = () }
  private[this] val NullGauge = new Gauge { def remove(): Unit = () }

  def counter(verbosity: Verbosity, name: String*): Counter = NullCounter
  def stat(verbosity: Verbosity, name: String*): Stat = NullStat
  def addGauge(verbosity: Verbosity, name: String*)(f: => Float): Gauge = NullGauge

  override def provideGauge(name: String*)(f: => Float): Unit = ()

  override def scope(namespace: String): StatsReceiver = this

  override def scopeSuffix(suffix: String): StatsReceiver = this

  override def isNull: Boolean = true

  override def toString: String = "NullStatsReceiver"
}
