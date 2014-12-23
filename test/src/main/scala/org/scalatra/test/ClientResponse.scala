package org.scalatra.test

import java.io.InputStream

import scala.collection.DefaultMap

case class ResponseStatus(code: Int, message: String)

import scala.collection.JavaConverters._

abstract class ClientResponse {
  def bodyBytes: Array[Byte]
  def inputStream: InputStream
  def statusLine: ResponseStatus
  def headers: Map[String, Seq[String]]

  def body = new String(bodyBytes, charset.getOrElse("ISO-8859-1"))

  def mediaType: Option[String] = {
    header.get("Content-Type") match {
      case Some(contentType) => contentType.split(";").map(_.trim).headOption
      case _ => None
    }
  }

  def status = statusLine.code

  val header = new DefaultMap[String, String] {
    def get(key: String) = {
      headers.get(key) match {
        case Some(values) => Some(values.head)
        case _ => None
      }
    }

    override def apply(key: String) = {
      get(key) match {
        case Some(value) => value
        case _ => null
      }
    }

    def iterator = {
      headers.keys.map(name => (name -> this(name))).iterator
    }
  }

  def charset = {
    header.getOrElse("Content-Type", "").split(";").map(_.trim).find(_.startsWith("charset=")) match {
      case Some(attr) => Some(attr.split("=")(1))
      case _ => None
    }
  }

  def getReason() = statusLine.message

  def getHeader(name: String) = header.getOrElse(name, null)

  def getLongHeader(name: String) = header.getOrElse(name, "-1").toLong

  def getHeaderNames(): java.util.Enumeration[String] = headers.keysIterator.asJavaEnumeration

  def getHeaderValues(name: String): java.util.Enumeration[String] = headers.getOrElse(name, Seq()).iterator.asJavaEnumeration

  def getContentBytes() = bodyBytes

  def getContent() = body

  def getContentType() = header.getOrElse("Content-Type", null)
}
