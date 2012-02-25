package org.scalatra

import java.nio.charset.Charset
import collection.mutable.ConcurrentMap
import java.io.{OutputStream, PrintWriter}
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConversions._
import scala.collection.mutable.Map

object ResponseStatus {
/*
  def apply(nettyStatus: HttpResponseStatus): ResponseStatus = 
    ResponseStatus(nettyStatus.getCode, nettyStatus.getReasonPhrase)
*/
}
case class ResponseStatus(code: Int, message: String = "TODO") extends Ordered[ResponseStatus] {

  def compare(that: ResponseStatus) = code.compareTo(that.code)

  def line = {
    val buf = new StringBuilder(message.length + 5);
    buf.append(code)
    buf.append(' ')
    buf.append(message)
    buf.toString()
  }
}

trait HttpResponse extends HttpMessage {
  
  // Implementing this as a Map in the Servlet API is awkward.
  // Consider an alternate approach.
  def headers: Map[String, String]

  def status: ResponseStatus
  def status_=(status: ResponseStatus)
  def contentType_=(contentType: Option[String])
  def characterEncoding_=(cs: Option[String])
//  def chunked: Boolean
//  def chunked_=(chunked: Boolean)

  def outputStream: OutputStream

  def writer: PrintWriter

  def redirect(uri: String)

  def end()

  def addHeader(name: String, value: String): Unit

  def setHeader(name: String, value: String): Unit
}
