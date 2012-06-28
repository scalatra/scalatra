package org.scalatra
package netty

import org.jboss.netty.handler.codec.http2.HttpHeaders.Names
import collection.JavaConversions._
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.CookieDecoder
import java.net.URI
import util.MultiMap
import util.io.PathManipulationOps
import java.io.InputStream
import collection.GenSeq
import org.jboss.netty.handler.codec.http2.QueryStringDecoder
import org.jboss.netty.handler.ssl.SslHandler
import io.backchat.http.ContentType
import scalax.io.Resource
import scala.io.Source


class NettyHttpRequest(
        initialMethod: HttpMethod,
        val uri: URI, 
        val headers: Map[String, String],
        contentTypeHeader: Option[ContentType],
        val queryString: String,
        postParameters: MultiMap,
        val files: GenSeq[HttpFile],
        val serverProtocol: HttpVersion,
        val inputStream: InputStream,
        private[netty] val ctx: ChannelHandlerContext)(implicit appContext: AppContext) extends HttpRequest {

  val pathInfo = uri.getPath.replaceFirst("^" + scriptName, "")

  val scriptName = PathManipulationOps.ensureSlash(appContext.server.base)

  import HeaderNames._

  attributes("org.scalatra.RequestMethod") = initialMethod
  def requestMethod: HttpMethod = attributes("org.scalatra.RequestMethod").asInstanceOf[HttpMethod]

  private[scalatra] def requestMethod_=(meth: HttpMethod) {
    attributes("org.scalatra.RequestMethod") = meth
  }

  val scheme = {
    headers.get(XForwardedProto).flatMap(_.blankOption) orElse {
      val fhs = headers.get(FrontEndHttps).flatMap(_.blankOption)
      fhs.filter(_.toLowerCase == "on").map(_ => "https")
    } getOrElse {
      if (ctx.getPipeline.get(classOf[SslHandler]) != null) "http" else "https"
    }
  }

  val cookies = {
    val nettyCookies = new CookieDecoder(true).decode(headers.getOrElse(Names.COOKIE, ""))
    val requestCookies =
      Map((nettyCookies map { nc =>
        val reqCookie: RequestCookie = nc
        reqCookie.name -> reqCookie
      }).toList:_*)
    new CookieJar(requestCookies)
  }

  val contentType = contentTypeHeader.map(_.mediaType.value).flatMap(_.blankOption)

  private def isWsHandshake =
    requestMethod == Get && headers.contains(Names.SEC_WEBSOCKET_KEY1) && headers.contains(Names.SEC_WEBSOCKET_KEY2)

  private def wsZero = if (isWsHandshake) Some(8L) else Some(0L)
  val contentLength =
    headers get Names.CONTENT_LENGTH flatMap (_.blankOption map (_.toLong) orElse wsZero)

  val serverName = appContext.server.name

  val serverPort = appContext.server.port


  /**
   * Http or Https, depending on the request URL.
   */
  val urlScheme: Scheme = Scheme(scheme)

  def isSecure: Boolean = urlScheme == Https

  /**
   * Caches and returns the body of the response.  The method is idempotent
   * for any given request.  The result is cached in memory regardless of size,
   * so be careful.  Calling this method consumes the request's input stream.
   *
   * @return the message body as a string according to the request's encoding
   * (defult ISO-8859-1).
   */
  def body:String = {
    cachedBody getOrElse {
      val encoding = characterEncoding getOrElse "ISO-8859-1"
      val body = Source.fromInputStream(inputStream, encoding).mkString
      update(CachedBodyKey, body)
      body
    }
  }

  private def cachedBody: Option[String] =
    get(CachedBodyKey).asInstanceOf[Option[String]]

  /**
   * The remote address the client is connected from.
   * This takes the load balancing header X-Forwarded-For into account
   * @return the client ip address
   */
  val remoteAddress: String = ctx.getChannel.getRemoteAddress.toString

  /**
   * Returns the name of the character encoding of the body, or None if no
   * character encoding is specified.
   */
  val characterEncoding: Option[String] = contentTypeHeader.flatMap(_.charset.map(_.value))

  //  val parameters = MultiMap(queryString ++ postParameters)
  val queryParams = MultiMap(new QueryStringDecoder(uri.toASCIIString).getParameters.mapValues(_.toSeq).toMap)

  attributes(MultiParamsKey) = MultiMap(queryParams ++ postParameters)
  /**
   * A Map of the parameters of this request. Parameters are contained in
   * the query string or posted form data.
   */
  val multiParameters: MultiParams = attributes(MultiParamsKey).asInstanceOf[MultiParams]

  protected[scalatra] def newResponse = new NettyHttpResponse(this, ctx)

}