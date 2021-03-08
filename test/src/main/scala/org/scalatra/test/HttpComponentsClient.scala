package org.scalatra.test

import java.io.{ ByteArrayOutputStream, File, OutputStream }

import org.apache.http.HttpResponse
import org.apache.http.client.CookieStore
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods._
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.content.{ ContentBody, StringBody }
import org.apache.http.entity.mime.{ FormBodyPartBuilder, HttpMultipartMode, MultipartEntityBuilder }
import org.apache.http.impl.client.{ BasicCookieStore, HttpClientBuilder }

import scala.util.DynamicVariable

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
      val (name, value) = (header.getName, header.getValue)
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
    queryParams: Iterable[(String, String)] = Seq.empty,
    headers: Iterable[(String, String)] = Seq.empty,
    body: Array[Byte] = null)(f: => A): A =
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
    headers: Iterable[(String, String)],
    files: Iterable[(String, Any)])(f: => A): A =
    {
      val client = createClient
      val url = "%s/%s".format(baseUrl, path)
      val req = createMethod(method.toUpperCase, url)

      attachMultipartBody(req, params, files)
      attachHeaders(req, headers)

      withResponse(HttpComponentsClientResponse(client.execute(req))) { f }
    }

  protected def createClient = {
    val builder = HttpClientBuilder.create()
    builder.disableRedirectHandling()
    if (_cookieStore.value != null) {
      builder.setDefaultCookieStore(_cookieStore.value)
    }
    builder.build()
  }

  private def attachHeaders(req: HttpRequestBase, headers: Iterable[(String, String)]): Unit = {
    headers.foreach { case (name, value) => req.addHeader(name, value) }
  }

  private def createMethod(method: String, url: String) = {
    val req = method match {
      case "GET" => new HttpGet(url)
      case "HEAD" => new HttpHead(url)
      case "OPTIONS" => new HttpOptions(url)
      case "DELETE" => new HttpDelete(url)
      case "TRACE" => new HttpTrace(url)
      case "POST" => new HttpPost(url)
      case "PUT" => new HttpPut(url)
      case "PATCH" => new HttpPatch(url)
    }

    req.setConfig(RequestConfig.custom().setCookieSpec("compatibility").build())

    req
  }

  private def attachBody(req: HttpRequestBase, body: Array[Byte]): Unit = {
    if (body == null) return

    req match {
      case r: HttpEntityEnclosingRequestBase =>
        r.setEntity(new ByteArrayEntity(body))

      case _ =>
        if (body.length > 0) {
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
    files: Iterable[(String, Any)]): Unit = {

    if (params.isEmpty && files.isEmpty) {
      return
    }

    req match {
      case r: HttpEntityEnclosingRequestBase =>
        val builder = MultipartEntityBuilder.create()
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        params.foreach {
          case (name, value) =>
            builder.addPart(FormBodyPartBuilder.create(name, new StringBody(value, ContentType.TEXT_PLAIN)).build())
        }

        files.foreach {
          case (name, file) =>
            builder.addPart(name, createBody(name, file))
        }

        r.setEntity(builder.build())

      case _ =>
        throw new IllegalArgumentException(
          """|HTTP %s does not support enclosing an entity.
             |Please remove the value from `body` parameter
             |or use POST/PUT/PATCH instead.""".stripMargin.format(req.getMethod))
    }
  }

  def createBody(name: String, content: Any) = content match {
    case file: File => UploadableBody(FilePart(file))
    case uploadable: Uploadable => UploadableBody(uploadable)

    case s: Any =>
      throw new IllegalArgumentException(
        ("The body type for file parameter '%s' could not be inferred. The " +
          "supported types are java.util.File and org.scalatra.test.Uploadable").format(name))
  }
}

case class UploadableBody(uploadable: Uploadable) extends ContentBody {
  def getMimeType = uploadable.contentType

  def getMediaType = "MULTIPART"

  def getSubType = "FORM-DATA"

  def getCharset = null

  def getTransferEncoding = "binary"

  def getContentLength = uploadable.contentLength

  def getFilename = uploadable.fileName

  def writeTo(out: OutputStream): Unit = {
    out.write(uploadable.content)
  }
}
