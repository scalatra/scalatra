package org.scalatra

object Scheme {
  def apply(sch: String) = sch.toLowerCase match {
    case "http" => Http
    case "https" => Https
  }
}
sealed trait Scheme

case object Http extends Scheme

case object Https extends Scheme

