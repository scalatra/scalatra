package org.scalatra

import java.io.{PrintWriter, OutputStream}
import collection.mutable

trait HttpResponse extends HttpMessage {
  /**
   * The HTTP status code and reason phrase.
   */
  def status: ResponseStatus
  def status_=(newStatus: ResponseStatus)

  def headers: mutable.Map[String, String]

  def contentType_=(contentType: String)

  def characterEncoding_=(cs: String)

  def outputStream: OutputStream

  private[scalatra] var usesWriter = false
  lazy val writer: PrintWriter = {
    usesWriter = true
    new PrintWriter(outputStream)
  }

  def redirect(uri: String)

  def end()

  def addHeader(name: String, value: String) {
    val newValue = headers.get(name) map { oldValue =>
      "%s,%s".format(oldValue, value)
    } getOrElse value
    headers(name) = newValue
  }

  def addCookie(cookie: Cookie)

  def chunked: Boolean
  def chunked_=(chunked: Boolean)

}
