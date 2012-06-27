package org.scalatra
package test

import org.scalatra.ResponseStatus
import java.io.InputStream
import java.net.URI
import scalax.io.{Codec => Codecx, Resource}
import java.nio.charset.Charset
import io.Codec

abstract class ClientResponse {

  def status: ResponseStatus
  def contentType: String
  def inputStream: InputStream
  def cookies: Map[String, HttpCookie]
  def headers: Map[String, String]
  def uri: URI

  private var _body: String = null
  private var _bodyBytes: Array[Byte] = null

  def statusCode = status.code
  def statusText = status.line
  def body = {
    if (_bodyBytes != null) new String(_bodyBytes, nioCharset)
    else {
      if (_body == null) _body = Resource.fromInputStream(inputStream).slurpString(Codecx(nioCharset))
      _body
    }
  }

  def bodyBytes = {
    if (_body != null) _body.getBytes(nioCharset)
    else {
      if (_bodyBytes == null) _bodyBytes = Resource.fromInputStream(inputStream).byteArray
      _bodyBytes
    }
  }

  private def nioCharset = charset map Charset.forName getOrElse Codec.UTF8
  def mediaType: Option[String] = headers.get("Content-Type") map { _.split(";")(0) }

  def charset: Option[String] =
    for {
      ct <- headers.get("Content-Type")
      charset <- ct.split(";").drop(1).headOption
    } yield charset.toUpperCase.replace("CHARSET=", "").trim
}

