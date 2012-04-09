package org.scalatra.fileupload

import javax.servlet.http.Part

case class FileItem(name: String, part: Part) {
  val size = part.getSize
  val fieldName = part.getName
  val contentType: Option[String] = Option(part.getHeader("content-type"))

  def bytes: Array[Byte] = org.scalatra.util.io.readBytes(getInputStream)
  def getInputStream = part.getInputStream
}
