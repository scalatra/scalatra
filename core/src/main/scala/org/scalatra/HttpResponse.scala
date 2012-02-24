package org.scalatra

import java.io.{OutputStream, PrintWriter}

trait HttpResponse extends HttpMessage {
  def status: Int
  def status_=(status: Int): Unit
  def status_=(status: (Int, String)): Unit

  def addHeader(name: String, value: String): Unit
  def setHeader(name: String, value: String): Unit

  def contentType_=(contentType: Option[String]): Unit
  def redirect(uri: String): Unit

  def outputStream: OutputStream
  def writer: PrintWriter
}
