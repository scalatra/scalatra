package org.scalatra

import java.nio.charset.Charset
import collection.mutable.ConcurrentMap
import java.io.{OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConversions._
import scala.collection.mutable.Map

trait Response extends HttpMessage {
  /**
   * The HTTP status code and reason phrase.
   */
  var status: ResponseStatus

  def headers: Map[String, String]

  def contentType_=(contentType: Option[String])

  def characterEncoding_=(cs: Option[String])

  def outputStream: OutputStream

  def writer: PrintWriter

  def redirect(uri: String)

  def end()

  def addHeader(name: String, value: String) {
    val newValue = headers.get(name) map { oldValue =>
      "%s,%s".format(oldValue, value)
    } getOrElse value
    headers(name) = newValue
  }

  def addCookie(cookie: Cookie): Unit

//  def chunked: Boolean
//  def chunked_=(chunked: Boolean)

}
