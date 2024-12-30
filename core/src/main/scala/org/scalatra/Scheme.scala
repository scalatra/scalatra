package org.scalatra

sealed trait Scheme

case object Http extends Scheme

case object Https extends Scheme
