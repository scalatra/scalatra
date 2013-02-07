package org.scalatra

import java.util.Locale

@deprecated("Moved to http4s", "3.0")
sealed trait HttpMethod {
  /**
   * Flag as to whether the method is "safe", as defined by RFC 2616.
   */
  val isSafe: Boolean
}
@deprecated("Moved to http4s", "3.0")
case object Options extends HttpMethod {
  val isSafe = true
  override def toString = "OPTIONS"
}
@deprecated("Moved to http4s", "3.0")
case object Get extends HttpMethod {
  val isSafe = true
  override def toString = "GET"
}
@deprecated("Moved to http4s", "3.0")
case object Head extends HttpMethod {
  val isSafe = true
  override def toString = "HEAD"
}
@deprecated("Moved to http4s", "3.0")
case object Post extends HttpMethod {
  val isSafe = false
  override def toString = "POST"
}
@deprecated("Moved to http4s", "3.0")
case object Put extends HttpMethod {
  val isSafe = false
  override def toString = "PUT"
}
@deprecated("Moved to http4s", "3.0")
case object Delete extends HttpMethod {
  val isSafe = false
  override def toString = "DELETE"
}
@deprecated("Moved to http4s", "3.0")
case object Trace extends HttpMethod {
  val isSafe = true
  override def toString = "TRACE"
}
@deprecated("Moved to http4s", "3.0")
case object Connect extends HttpMethod {
  val isSafe = false
  override def toString = "CONNECT"
}
@deprecated("Moved to http4s", "3.0")
case object Patch extends HttpMethod {
  val isSafe = false
  override def toString = "PATCH"
}
@deprecated("Moved to http4s", "3.0")
case class ExtensionMethod(name: String) extends HttpMethod {
  val isSafe = false
}

@deprecated("Moved to http4s", "3.0")
object HttpMethod {
  private[this] val methodMap =
    Map(List(Options, Get, Head, Post, Put, Delete, Trace, Connect, Patch) map {
      method => (method.toString, method)
    } : _*)

  /**
   * Maps a String as an HttpMethod.
   *
   * @param name a string representing an HttpMethod
   * @return the matching common HttpMethod, or an instance of `ExtensionMethod`
   * if no method matches
   */
  @deprecated("Moved to http4s", "3.0")
  def apply(name: String): HttpMethod = {
    val canonicalName = name.toUpperCase(Locale.ENGLISH)
    methodMap.getOrElse(canonicalName, ExtensionMethod(canonicalName))
  }

  /**
   * The set of common HTTP methods: GET, HEAD, POST, PUT, DELETE, TRACE,
   * CONNECT, and PATCH.
   */
  @deprecated("Moved to http4s", "3.0")
  val methods: Set[HttpMethod] = methodMap.values.toSet
}
