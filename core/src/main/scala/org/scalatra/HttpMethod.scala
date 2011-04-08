package org.scalatra

import java.util.Locale

sealed trait HttpMethod {
  val isSafe: Boolean
}
case object Options extends HttpMethod {
  val isSafe = true
  override def toString = "OPTIONS"
}
case object Get extends HttpMethod {
  val isSafe = true
  override def toString = "GET"
}
case object Head extends HttpMethod {
  val isSafe = true
  override def toString = "HEAD"
}
case object Post extends HttpMethod {
  val isSafe = false
  override def toString = "POST"
}
case object Put extends HttpMethod {
  val isSafe = false
  override def toString = "PUT"
}
case object Delete extends HttpMethod {
  val isSafe = false
  override def toString = "DELETE"
}
case object Trace extends HttpMethod {
  val isSafe = true
  override def toString = "TRACE"
}
case object Connect extends HttpMethod {
  val isSafe = false
  override def toString = "CONNECT"
}
case class ExtensionMethod(name: String) extends HttpMethod {
  val isSafe = false
}

object HttpMethod {
  private val methodMap =
    Map(List(Options, Get, Head, Post, Put, Delete, Trace, Connect) map {
      method => (method.toString, method)
    } : _*)

  def apply(name: String): HttpMethod = {
    val canonicalName = name.toUpperCase(Locale.ENGLISH)
    methodMap.getOrElse(canonicalName, ExtensionMethod(canonicalName))
  }

  val methods: Set[HttpMethod] = methodMap.values.toSet
}
