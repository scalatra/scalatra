package org.scalatra.test

import org.apache.http.impl.client.DefaultHttpClient
import dispatch.url
import org.apache.http.HttpResponse
import org.apache.http.client.methods._
import org.apache.http.entity.ByteArrayEntity
import java.io.{File, ByteArrayOutputStream}

trait HttpComponentsClient extends Client {
  def baseUrl: String

  type Response = HttpResponse

  def header = response.getAllHeaders.map(header => (header.getName, header.getValue)).toMap

  def status = response.getStatusLine.getStatusCode

  def body = new String(bodyBytes, "iso-8859-1")

  def bodyBytes = {
    val bos = new ByteArrayOutputStream()
    response.getEntity.writeTo(bos)

    bos.toByteArray
  }

  def submit[A](
    method: String,
    path: String,
    queryParams: Iterable[(String, String)] = Map.empty,
    headers: Map[String, String] = Map.empty,
    body: String = null)(f: => A): A =
  {
    val client = new DefaultHttpClient()

    val queryString = toQueryString(queryParams)
    val url = if (queryString == "")
      "%s/%s".format(baseUrl, path)
    else
      "%s/%s?%s".format(baseUrl, path, queryString)

    val req = createMethod(method.toUpperCase, url)
    attachBody(req, body)
    headers.foreach { case (name, value) => req.setHeader(name, value) }

    withResponse(client.execute(req)) { f }
  }



  private def createMethod(method: String, url: String) = {
    method match {
      case "GET"     => new HttpGet(url)
      case "HEAD"    => new HttpHead(url)
      case "OPTIONS" => new HttpOptions(url)
      case "DELETE"  => new HttpDelete(url)
      case "TRACE"   => new HttpTrace(url)
      case "POST"  => new HttpPost(url)
      case "PUT"   => new HttpPut(url)
      case "PATCH" => new HttpPatch(url)
    }
  }

  private def attachBody(req: HttpRequestBase, body: String) {
    val bodyBytes = Option(body).getOrElse("").getBytes("iso-8859-1")

    req match {
      case r: HttpEntityEnclosingRequestBase =>
        r.setEntity(new ByteArrayEntity(bodyBytes))

      case _ =>
        if (bodyBytes.length > 0) {
          throw new IllegalArgumentException(
            """|HTTP %s does not support enclosing an entity.
               |Please remove the value from `body` parameter
               |or use POST/PUT/PATCH instead.""".stripMargin.format(req.getMethod))
        }
    }
  }

  protected def submitMultipart[A](method: String, uri: String, params: Iterable[(String, String)], headers: Map[String, String], files: Iterable[(String, File)])(f: => A) =
    throw new UnsupportedOperationException()

}
