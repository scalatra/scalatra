package org.scalatra.metrics

import java.util.concurrent.Callable
import com.codahale.metrics._
import nl.grons.metrics.scala._
import org.scalatra.ScalatraBase

trait MetricsSupport {
  self: ScalatraBase ⇒

  protected def metricsRegistry = Metrics.metricRegistry

  def metricName(name: String) = MetricName(name)

  def timer[A](name: String)(thunk: => A) = metricsRegistry.timer(name).time { new Callable[A] { def call(): A = thunk } }
  def counter(name: String) = metricsRegistry.counter(name)
  def histogram(name: String) = metricsRegistry.histogram(name)
  def meter(name: String) = metricsRegistry.meter(name)
}