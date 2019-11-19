package com.twitter.finagle.stats

/**
 * Represents the "role" this service plays with respect to this metric.
 *
 * Usually either Server (the service is processing a request) or Client (the server has sent a
 * request to another service). In many cases, there is no relevant role for a metric, in which
 * case NoRole should be used.
 */
sealed trait SourceRole
case object NoRoleSpecified extends SourceRole
case object Client extends SourceRole
case object Server extends SourceRole

/**
 * A builder class used to configure settings and metadata for metrics prior to instantiating them.
 * Calling any of the three build methods (counter, gauge, or histogram) will cause the metric to be
 * instantiated in the underlying StatsReceiver.
 *
 * @param keyIndicator indicates whether this metric is crucial to this service (ie, an SLO metric)
 * @param description human-readable description of a metric's significance
 * @param units the unit associated with the metrics value (milliseconds, megabytes, requests, etc)
 * @param role whether the service is playing the part of client or server regarding this metric
 * @param verbosity see StatsReceiver for details
 * @param sourceClass the class pass of the module which generated this metric (ie, com.twitter...)
 * @param sourceLibrary the name of the library which generated this metric (ie, finagle)
 * @param name the relative metric name which will be appended to the scope of the StatsReceiver prior to long term storage
 * @param identifier the service name used by obs stack for this service
 * @param percentiles used to indicate buckets for histograms, to be set by the StatsReceiver
 * @param statsReceiver used for the actual metric creation, set by the StatsReceiver when creating a MetricBuilder
 */
class MetricBuilder(
  val keyIndicator: Boolean = false,
  val description: String = "",
  val units: MetricUnit = Unspecified,
  val role: SourceRole = NoRoleSpecified,
  val verbosity: Verbosity = Verbosity.Default,
  val sourceClass: String = "",
  val sourceLibrary: Option[String] = None,
  val name: Seq[String] = Seq(),
  val identifier: String = "",
  // Only persisted and relevant when building histograms.
  val percentiles: IndexedSeq[Double] = IndexedSeq(),
  val statsReceiver: StatsReceiver) {

  /**
   * This copy method omits statsReceiver and percentiles as arguments because they should be
   * set only during the initial creation of a MetricBuilder by a StatsReceiver, which should use
   * itself as the value for statsReceiver.
   */
  private[this] def copy(
    keyIndicator: Boolean = this.keyIndicator,
    description: String = this.description,
    units: MetricUnit = this.units,
    role: SourceRole = this.role,
    verbosity: Verbosity = this.verbosity,
    sourceClass: String = this.sourceClass,
    sourceLibrary: Option[String] = this.sourceLibrary,
    name: Seq[String] = this.name,
    identifier: String = this.identifier
  ): MetricBuilder = {
    new MetricBuilder(
      keyIndicator = keyIndicator,
      description = description,
      units = units,
      role = role,
      verbosity = verbosity,
      sourceClass = sourceClass,
      sourceLibrary = sourceLibrary,
      name = name,
      identifier = identifier,
      percentiles = this.percentiles,
      statsReceiver = this.statsReceiver
    )
  }

  def withKeyIndicator(isKeyIndicator: Boolean = true): MetricBuilder =
    this.copy(keyIndicator = isKeyIndicator)

  def withDescription(desc: String): MetricBuilder = this.copy(description = desc)

  def withVerbosity(verbosity: Verbosity): MetricBuilder = this.copy(verbosity = verbosity)

  def withSourceClass(sourceClass: String): MetricBuilder = this.copy(sourceClass = sourceClass)

  def withSourceLibrary(sourceLibrary: Option[String]): MetricBuilder =
    this.copy(sourceLibrary = sourceLibrary)

  def withIdentifier(identifier: String): MetricBuilder = this.copy(identifier = identifier)

  def withUnits(units: MetricUnit): MetricBuilder = this.copy(units = units)

  def withRole(role: SourceRole): MetricBuilder = this.copy(role = role)

  /**
   * Generates a CounterSchema which can be used to create a counter in a StatsReceiver.
   * Used to test that builder class correctly propagates configured metadata.
   * @return a CounterSchema describing a counter.
   */
  private[MetricBuilder] def counterSchema: CounterSchema = CounterSchema(this)

  /**
   * Generates a GaugeSchema which can be used to create a gauge in a StatsReceiver.
   * Used to test that builder class correctly propagates configured metadata.
   * @return a GaugeSchema describing a gauge.
   */
  private[MetricBuilder] def gaugeSchema: GaugeSchema = GaugeSchema(this)

  /**
   * Generates a HistogramSchema which can be used to create a histogram in a StatsReceiver.
   * Used to test that builder class correctly propagates configured metadata.
   * @return a HistogramSchema describing a histogram.
   */
  private[MetricBuilder] def histogramSchema: HistogramSchema = HistogramSchema(this)

  /**
   * Produce a counter as described by the builder inside the underlying StatsReceiver.
   * @return the counter created.
   */
  def counter(name: String*): Counter = {
    val schema = this.copy(name = name).counterSchema
    this.statsReceiver.counter(schema)
  }

  /**
   * Produce a gauge as described by the builder inside the underlying StatsReceiver.
   * @return the gauge created.
   */
  def gauge(name: String*)(f: => Float): Gauge = {
    val schema = this.copy(name = name).gaugeSchema
    this.statsReceiver.addGauge(schema)(f)
  }

  /**
   * Produce a histogram as described by the builder inside the underlying StatsReceiver.
   * @return the histogram created.
   */
  def histogram(name: String*): Stat = {
    val schema = this.copy(name = name).histogramSchema
    this.statsReceiver.stat(schema)
  }
}
