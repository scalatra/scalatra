package org.scalatra
package http

import java.io.{OutputStream, PrintWriter}

trait HttpResponse[A] extends HttpMessage[A] {
  def default: A

  def status(implicit a: A): Int
  def status_=(status: Int)(implicit a: A): Unit
  def status_=(status: (Int, String))(implicit a: A): Unit

  def addHeader(name: String, value: String)(implicit a: A): Unit
  def setHeader(name: String, value: String)(implicit a: A): Unit

  def contentType_=(contentType: Option[String])(implicit a: A): Unit
  def redirect(uri: String)(implicit a: A): Unit

  def outputStream(implicit a: A): OutputStream
  def writer(implicit a: A): PrintWriter
}
