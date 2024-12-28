package org.scalatra.metrics

import java.util.concurrent.Callable

import nl.grons.metrics4.scala._

trait MetricsSupport extends InstrumentedBuilder with MetricsBootstrap {

  def metricName(name: String) = MetricName(name)

  def timer[A](name: String)(thunk: => A) =
    metrics.timer(name).time(new Callable[A] { def call(): A = thunk })
  def gauge[A](name: String)(thunk: => A) = metrics.gauge(name) {
    new Callable[A] { def call(): A = thunk }
  }
  def counter(name: String) = metrics.counter(name)
  def histogram(name: String) = metrics.histogram(name)
  def meter(name: String) = metrics.meter(name)
}
