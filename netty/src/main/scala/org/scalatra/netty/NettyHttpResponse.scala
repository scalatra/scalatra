package org.scalatra
package netty

import scala.io.Codec
import org.jboss.netty.handler.codec.http2.HttpHeaders.Names
import org.jboss.netty.channel.{ChannelFutureListener, ChannelHandlerContext}
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferOutputStream}
import org.jboss.netty.handler.codec.http2.{HttpHeaders, DefaultHttpResponse, HttpResponseStatus, HttpVersion => JHttpVersion}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._

object NettyHttpResponse {
  val EncodingKey = "org.scalatra.response.encoding"
}
class NettyHttpResponse(request: NettyHttpRequest, connection: ChannelHandlerContext) extends HttpResponse {

  import NettyHttpResponse._

  private val _ended = new AtomicBoolean(false)
  val underlying = new DefaultHttpResponse(nettyProtocol, HttpResponseStatus.OK)
  val headers: collection.mutable.Map[String, String] = new ConcurrentHashMap[String, String]().asScala
  private def nettyProtocol = request.serverProtocol match {
    case Http10 => JHttpVersion.HTTP_1_0
    case Http11 => JHttpVersion.HTTP_1_1
  }

  def status = underlying.getStatus
  def status_=(status: ResponseStatus) = underlying.setStatus(status)

  def contentType = {
    headers.get(Names.CONTENT_TYPE).flatMap(_.blankOption)
  }
  def contentType_=(ct: String) = headers(Names.CONTENT_TYPE) = ct

  // TODO: get this stuff from some headers
  def characterEncoding = request.get(EncodingKey).flatMap(_.toString.blankOption) orElse Some(Codec.UTF8.name)
  def characterEncoding_=(enc: String) = request(EncodingKey) = enc

  val outputStream  = new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer())

  def end() = {
    if (_ended.compareAndSet(false, true)) {
      headers foreach {
        case (k, v) if k == Names.CONTENT_TYPE => {
          val Array(mediaType, hdrCharset) = {
            val parts = v.split(';').map(_.trim)
            if (parts.size > 1) parts else Array(parts(0), "")
          }

          underlying.setHeader(k, mediaType + ";" + (hdrCharset.blankOption getOrElse "charset=%s".format(characterEncoding.orNull)))
        }
        case (k, v) => {
          underlying.setHeader(k, v)
        }
      }
      request.cookies.responseCookies foreach { cookie => underlying.addHeader(Names.SET_COOKIE, cookie.toCookieString) }
      val content = outputStream.buffer()
      if (content.readableBytes() < 1) content.writeByte(0x1A)
      underlying.setContent(content)
      val fut = connection.getChannel.write(underlying)
      if(!HttpHeaders.isKeepAlive(underlying) || !chunked) fut.addListener(ChannelFutureListener.CLOSE)
    }
  }

  def chunked = underlying.isChunked

  def chunked_=(chunked: Boolean) = underlying setChunked chunked

  def redirect(uri: String) = {
    underlying.setStatus(HttpResponseStatus.FOUND)
    underlying.setHeader(Names.LOCATION, uri)
    end()
  }

  def addCookie(cookie: Cookie) {
    request.cookies.update(cookie.name, cookie.value)(cookie.cookieOptions)
  }
}