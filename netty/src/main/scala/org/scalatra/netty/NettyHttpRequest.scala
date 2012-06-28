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


class NettyHttpRequest(
        val requestMethod: HttpMethod,
        val uri: URI, 
        val headers: Map[String, String],
        val queryString: String,
        postParameters: MultiMap,
        val files: GenSeq[HttpFile],
        val serverProtocol: HttpVersion,
        val inputStream: InputStream,
        private[netty] val ctx: ChannelHandlerContext)(implicit appContext: AppContext) extends HttpRequest {

  val pathInfo = uri.getPath.replaceFirst("^" + scriptName, "")

  val scriptName = PathManipulationOps.ensureSlash(appContext.server.base)

  val scheme = "http" //uri.getScheme

  val cookies = {
    val nettyCookies = new CookieDecoder(true).decode(headers.getOrElse(Names.COOKIE, ""))
    val requestCookies =
      Map((nettyCookies map { nc =>
        val reqCookie: RequestCookie = nc
        reqCookie.name -> reqCookie
      }).toList:_*)
    new CookieJar(requestCookies)
  }

  val contentType = headers.get(Names.CONTENT_TYPE).flatMap(_.blankOption)

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
  val urlScheme: Scheme = Scheme("http") //Scheme(uri.getScheme)  // TODO: Take X-Forwarded-Proto into account

  def isSecure: Boolean = urlScheme == Https

  /**
   * Caches and returns the body of the response.  The method is idempotent
   * for any given request.  The result is cached in memory regardless of size,
   * so be careful.  Calling this method consumes the request's input stream.
   *
   * @return the message body as a string according to the request's encoding
   *         (defult ISO-8859-1).
   */
  def body: String = null

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
  def characterEncoding: Option[String] = None

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