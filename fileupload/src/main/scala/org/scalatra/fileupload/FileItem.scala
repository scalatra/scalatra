package org.scalatra.fileupload

import javax.servlet.http.Part
import java.io.File

case class FileItem(part: Part) {
  val size = part.getSize
  val fieldName = part.getName
  val name = Util.partAttribute(part, "content-disposition", "filename")
  val contentType: Option[String] = Option(part.getContentType)
  val charset: Option[String] = Option(Util.partAttribute(part, "content-type", "charset"))

  def getName = name
  def getFieldName = fieldName
  def getSize = size
  def getContentType = contentType.orElse(null)
  def getCharset = charset.orElse(null)

  def write(file: File) { part.write(file.getPath) }
  def write(fileName: String) { part.write(fileName) }

  def get() = org.scalatra.util.io.readBytes(getInputStream)
  def isFormField = (name == null)
  def getInputStream = part.getInputStream
}
