package org.scalatra.test

import java.io.InputStream

case class ResponseStatus(code: Int, message: String)

import scala.jdk.CollectionConverters._

abstract class ClientResponse {
  def bodyBytes: Array[Byte]
  def inputStream: InputStream
  def statusLine: ResponseStatus
  def headers: Map[String, Seq[String]]

  def body: String = {
    val charsetName = charset
      .orElse {
        if (mediaType.contains("application/json")) Some("UTF-8") else None
      }
      .getOrElse("ISO-8859-1")
    new String(bodyBytes, charsetName)
  }

  def mediaType: Option[String] =
    header.get("Content-Type") match {
      case Some(contentType) => contentType.split(";").map(_.trim).headOption
      case _                 => None
    }

  def status: Int = statusLine.code

  object header {

    def get(key: String): Option[String] =
      headers.get(key) match {
        case Some(values) => Some(values.head)
        case _            => None
      }

    def getOrElse(key: String, default: => String): String =
      get(key) getOrElse (default)

    def apply(key: String): String =
      get(key) match {
        case Some(value) => value
        case _           => null
      }

    def iterator: Iterator[(String, String)] =
      headers.keys.map(name => (name -> this(name))).iterator
  }

  def charset: Option[String] =
    header
      .getOrElse("Content-Type", "")
      .split(";")
      .map(_.trim)
      .find(_.startsWith("charset=")) match {
      case Some(attr) => Some(attr.split("=")(1))
      case _          => None
    }

  def getReason(): String = statusLine.message

  def getHeader(name: String): String = header.getOrElse(name, null)

  def getLongHeader(name: String): Long = header.getOrElse(name, "-1").toLong

  def getHeaderNames(): java.util.Enumeration[String] =
    headers.keysIterator.asJavaEnumeration

  def getHeaderValues(name: String): java.util.Enumeration[String] =
    headers.getOrElse(name, Seq()).iterator.asJavaEnumeration

  def getContentBytes(): Array[Byte] = bodyBytes

  def getContent(): String = body

  def getContentType(): String = header.getOrElse("Content-Type", null)
}
