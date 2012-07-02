package org.scalatra.test

import org.apache.http.impl.client.{BasicCookieStore, DefaultHttpClient}
import org.apache.http.HttpResponse
import org.apache.http.client.methods._
import org.apache.http.entity.ByteArrayEntity
import java.io.{OutputStream, File, ByteArrayOutputStream}
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import org.apache.http.entity.mime.{FormBodyPart, MultipartEntity, HttpMultipartMode}
import org.apache.http.entity.mime.content.{ContentBody, FileBody, StringBody}
import util.DynamicVariable
import org.apache.http.client.CookieStore

case class HttpComponentsClientResponse(res: HttpResponse) extends ClientResponse {
  lazy val bodyBytes: Array[Byte] = {
    Option(res.getEntity) match {
      case Some(entity) =>
        val bos = new ByteArrayOutputStream()
        res.getEntity.writeTo(bos)

        bos.toByteArray

      case None => Array()
    }
  }

  def inputStream = res.getEntity.getContent

  def statusLine = {
    val sl = res.getStatusLine
    ResponseStatus(sl.getStatusCode, sl.getReasonPhrase)
  }

  def headers = {
    res.getAllHeaders.foldLeft(Map[String, Seq[String]]()) { (hmap, header) =>
      val Array(name, value) = Array(header.getName, header.getValue)
      val values = hmap.getOrElse(name, Seq())

      hmap + (name -> (values :+ value))
    }
  }
}

trait HttpComponentsClient extends Client {
  def baseUrl: String

  private val _cookieStore = new DynamicVariable[CookieStore](null)

  def session[A](f: => A): A = {
    _cookieStore.withValue(new BasicCookieStore) { f }
  }

  def submit[A](
    method: String,
    path: String,
    queryParams: Iterable[(String, String)] = Map.empty,
    headers: Map[String, String] = Map.empty,
    body: String = null)(f: => A): A =
  {
    val client = createClient
    val queryString = toQueryString(queryParams)
    val url = if (queryString == "")
      "%s/%s".format(baseUrl, path)
    else
      "%s/%s?%s".format(baseUrl, path, queryString)

    val req = createMethod(method.toUpperCase, url)
    attachBody(req, body)
    attachHeaders(req, headers)

    withResponse(HttpComponentsClientResponse(client.execute(req))) { f }
  }

  protected def submitMultipart[A](
    method: String,
    path: String,
    params: Iterable[(String, String)],
    headers: Map[String, String],
    files: Iterable[(String, Any)])(f: => A): A =
  {
    val client = createClient
    val url = "%s/%s".format(baseUrl, path)
    val req = createMethod(method.toUpperCase, url)

    attachMultipartBody(req, params, files)
    attachHeaders(req, headers)

    withResponse(HttpComponentsClientResponse(client.execute(req))) { f }
  }

  private def createClient = {
    val client = new DefaultHttpClient()
    client.getParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false)
    client.getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)

    if (_cookieStore.value != null) {
      client.setCookieStore(_cookieStore.value)
    }

    client
  }

  private def attachHeaders(req: HttpRequestBase, headers: Map[String, String]) {
    headers.foreach { case (name, value) => req.setHeader(name, value) }
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

  private def attachMultipartBody(
    req: HttpRequestBase,
    params: Iterable[(String, String)],
    files: Iterable[(String, Any)])
  {

    if (params.isEmpty && files.isEmpty) {
      return
    }

    req match {
      case r: HttpEntityEnclosingRequestBase =>
        val multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
        params.foreach {
          case (name, value) =>
            multipartEntity.addPart(new FormBodyPart(name, new StringBody(value)))
        }

        files.foreach {
          case (name, file) =>
            multipartEntity.addPart(name, createBody(file))
        }

        r.setEntity(multipartEntity)

      case _ =>
        throw new IllegalArgumentException(
          """|HTTP %s does not support enclosing an entity.
             |Please remove the value from `body` parameter
             |or use POST/PUT/PATCH instead.""".stripMargin.format(req.getMethod))
    }
  }

  private def createBody(part: Any): ContentBody = part match {
    case file: File => new FileBody(file)
    case _          => new StringBody(part.toString)
  }
}

case class UploadableBody(uploadable: Uploadable) extends ContentBody {
  def getMimeType = uploadable.contentType

  def getMediaType = "MULTIPART"

  def getSubType = "FORM-DATA"

  def getCharset = null

  def getTransferEncoding = "binary"

  def getContentLength = 0L

  def getFilename = ""

  def writeTo(out: OutputStream) {}
}