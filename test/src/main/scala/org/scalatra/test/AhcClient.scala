package org.scalatra
package test

import com.ning.http.client._
import collection.JavaConversions._
import org.scalatra.util.io.{FileCharset, Mimes}
import java.util.Locale._
import java.nio.charset.Charset
import scala.io.Codec
import java.io.File
import java.net.URI
import java.util.Locale
import rl.MapQueryString
import org.scalatra.CookieOptions
import org.jboss.netty.handler.codec.http.HttpHeaders.Names

class AhcClientResponse(response: Response) extends ClientResponse {
  val cookies = (response.getCookies map { cookie =>
    val cko = CookieOptions(cookie.getDomain, cookie.getPath, cookie.getMaxAge)
    cookie.getName -> org.scalatra.Cookie(cookie.getName, cookie.getValue)(cko)
  }).toMap

  val headers = (response.getHeaders.keySet() map { k => k -> response.getHeaders(k).mkString("; ")}).toMap

  val status = ResponseStatus(response.getStatusCode, response.getStatusText)

  val contentType = response.getContentType

  val inputStream = response.getResponseBodyAsStream

  val uri = response.getUri
}

private[test] object StringHttpMethod {
  val GET = "GET"
  val POST = "POST"
  val DELETE = "DELETE"
  val PUT = "PUT"
  val CONNECT = "CONNECT"
  val HEAD = "HEAD"
  val OPTIONS = "OPTIONS"
  val PATCH = "PATCH"
  val TRACE = "TRACE"
}

class AhcClient(val host: String, val port: Int) extends Client {
  import StringHttpMethod._
  private val clientConfig = new AsyncHttpClientConfig.Builder().setFollowRedirects(false).build()
  private val underlying = new AsyncHttpClient(clientConfig) {
    def preparePatch(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder("PATCH", uri)
    def prepareTrace(uri: String): AsyncHttpClient#BoundRequestBuilder = requestBuilder("TRACE", uri)
  }

  private val mimes = new Mimes {
    protected def warn(message: String) = System.err.println("[WARN] " + message)
  }

  override def stop() {
    underlying.close()
  }

  private def requestFactory(method: String): String ⇒ AsyncHttpClient#BoundRequestBuilder = {
    method.toUpperCase(ENGLISH) match {
      case `GET`     ⇒ underlying.prepareGet _
      case `POST`    ⇒ underlying.preparePost _
      case `PUT`     ⇒ underlying.preparePut _
      case `DELETE`  ⇒ underlying.prepareDelete _
      case `HEAD`    ⇒ underlying.prepareHead _
      case `OPTIONS` ⇒ underlying.prepareOptions _
      case `CONNECT` ⇒ underlying.prepareConnect _
      case `PATCH`   ⇒ underlying.preparePatch _
      case `TRACE`   ⇒ underlying.prepareTrace _
    }
  }

  private def addParameters(method: String, params: Iterable[(String, String)], isMultipart: Boolean = false, charset: Charset = Codec.UTF8)(req: AsyncHttpClient#BoundRequestBuilder) = {
    method.toUpperCase(ENGLISH) match {
      case `GET` | `DELETE` | `HEAD` | `OPTIONS` ⇒ params foreach { case (k, v) ⇒ req addQueryParameter (k, v) }
      case `PUT` | `POST`   | `PATCH`            ⇒ {
        if (!isMultipart)
          params foreach { case (k, v) ⇒ req addParameter (k, v) }
        else {
          params foreach { case (k, v) => req addBodyPart new StringPart(k, v, charset.name)}
        }
      }
      case _                                     ⇒ // we don't care, carry on
    }
    req
  }

  private def addHeaders(headers: Iterable[(String, String)])(req: AsyncHttpClient#BoundRequestBuilder) = {
    headers foreach { case (k, v) => req.addHeader(k, v) }
    req
  }

  private val allowsBody = Vector(PUT, POST, PATCH)

  def submit[A](method: String, uri: String, params: Iterable[(String, String)], headers: Iterable[(String, String)], files: Seq[File], body: String)(f: => A) = {
    val u = URI.create(uri)
    val isMultipart = {
      allowsBody.contains(method.toUpperCase(Locale.ENGLISH)) && {
        val ct = (defaultWriteContentType(files) ++ headers)(Names.CONTENT_TYPE)
        ct.toLowerCase(Locale.ENGLISH).startsWith("multipart/form-data")
      }
    }
    val reqUri = if (u.isAbsolute) u else new URI("http", null, host, port, u.getPath, u.getQuery, u.getFragment)
    val req = (requestFactory(method)
      andThen (addHeaders(headers) _)
      andThen (addParameters(method, params, isMultipart) _))(reqUri.toASCIIString)
    if (isMultipart) {
      files foreach { file =>
        req.addBodyPart(new FilePart(file.getName, file, mimes(file), FileCharset(file).name))
      }
    }
    if (useSession && cookies.size > 0) {
      cookies foreach { cookie =>
        val ahcCookie = new Cookie(
          cookie.cookieOptions.domain,
          cookie.name, cookie.value,
          cookie.cookieOptions.path,
          cookie.cookieOptions.maxAge,
          cookie.cookieOptions.secure)
        req.addCookie(ahcCookie)
      }
    }
    u.getQuery.blankOption foreach { uu =>
      MapQueryString.parseString(uu) foreach { case (k, v) => v foreach { req.addQueryParameter(k, _) } }
    }
    if (allowsBody.contains(method.toUpperCase(ENGLISH)) && body.nonBlank) req.setBody(body)
    val res = req.execute(async).get
    withResponse(res)(f)
  }



  private def async = new AsyncCompletionHandler[ClientResponse] {

    override def onThrowable(t: Throwable) {
      t.printStackTrace()
    }

    def onCompleted(response: Response) = new AhcClientResponse(response)
  }
}
