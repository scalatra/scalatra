package org.scalatra.metrics

import java.util.concurrent.Callable
import com.codahale.metrics._
import nl.grons.metrics.scala._
import org.scalatra.ScalatraBase

trait MetricsSupport extends nl.grons.metrics.scala.InstrumentedBuilder {
  self: ScalatraBase ⇒

  def metricsRegistry = Metrics.metricRegistry

  def metricName(name: String) = MetricName(name)

  def timer[A](name: String)(thunk: => A) = metrics.timer(name).time { new Callable[A] { def call(): A = thunk } }
  def gauge[A](name: String)(thunk: => A) = metrics.gauge(name) { new Callable[A] { def call(): A = thunk } }
  def counter(name: String) = metrics.counter(name)
  def histogram(name: String) = metrics.histogram(name)
  def meter(name: String) = metrics.meter(name)
}