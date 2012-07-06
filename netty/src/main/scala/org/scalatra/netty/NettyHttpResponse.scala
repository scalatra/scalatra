package org.scalatra
package netty

import org.jboss.netty.handler.codec.http2.HttpHeaders.{Values, Names}
import org.jboss.netty.channel.{ChannelFutureListener, ChannelHandlerContext}
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferOutputStream}
import org.jboss.netty.handler.codec.http2.{HttpHeaders, DefaultHttpResponse, HttpResponseStatus, HttpVersion => JHttpVersion}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._
import org.scalatra.Cookie

object NettyHttpResponse {
  val EncodingKey = "org.scalatra.response.encoding"
  val ContentHeaderKey = "org.scalatra.contentTypeHeader"
  val ParsedContentHeaderKey = "org.scalatra.parsedContentTypeHeader"
}
class NettyHttpResponse(request: NettyHttpRequest, connection: ChannelHandlerContext) extends HttpResponse {

  import NettyHttpResponse._

  private val _ended = new AtomicBoolean(false)
  val underlying = new DefaultHttpResponse(nettyProtocol, HttpResponseStatus.OK)
  val headers: collection.mutable.Map[String, String] = new ConcurrentHashMap[String, String]().asScala

  def serverProtocol = request.serverProtocol

  private def nettyProtocol = request.serverProtocol match {
    case Http10 => JHttpVersion.HTTP_1_0
    case Http11 => JHttpVersion.HTTP_1_1
  }

  def status = underlying.getStatus
  def status_=(status: ResponseStatus) = underlying.setStatus(status)

  def contentType = {
    headers.get(Names.CONTENT_TYPE).flatMap(_.blankOption)
  }
  def contentType_=(ct: String) = {
    headers(Names.CONTENT_TYPE) = ct
//    request(ContentHeaderKey) = ct
//    request(ParsedContentHeaderKey) = null
  }

//  private def contentTypeHeader = {
//    import io.backchat.http.{HttpHeaders => BH}
//    val cacheInvalid = request.get(ContentHeaderKey) == contentType
//    val rule = HttpParser.rules("CONTENT_TYPE")
//    val cached =
//      if (cacheInvalid) None else request.get(ParsedContentHeaderKey).flatMap(Option(_)).map(_.asInstanceOf[ContentType])
//    cached orElse {
//
//      contentType flatMap { ct =>
//        ReportingParseRunner(rule).run(ct).result match {
//          case Some(BH.`Content-Type`(ctt)) => {
//            request(ParsedContentHeaderKey) = ctt
//            Some(ctt)
//          }
//          case _ => None
//        }
//      }
//    }
//  }

  def characterEncoding = {
//    contentTypeHeader.flatMap(_.charset.map(_.value)) orElse request.get(EncodingKey).flatMap(_.toString.blankOption)
    val hdrCharset = contentType flatMap { ct =>
      val parts = ct.split(';').map(_.trim)
      val cs = if (parts.size > 1) parts(1) else ""
      cs.blankOption.flatMap(_.toUpperCase.replace("CHARSET=", "").trim.blankOption)
    }
    hdrCharset orElse request.get(EncodingKey).flatMap(_.toString.blankOption)
  }
  def characterEncoding_=(enc: String) = request(EncodingKey) = enc

  val outputStream  = new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer())

  def end() = {
    if (_ended.compareAndSet(false, true)) {
      headers foreach {
        case (k, v) if k == Names.CONTENT_TYPE => {
          val mediaType = {
            val parts = v.split(';').map(_.trim)
            parts(0)
          }
          val appendCharset = characterEncoding.map("; charset="+_).orNull
          underlying.setHeader(k, mediaType.blankOption.getOrElse("text/plain") + appendCharset)
        }
        case (k, v) => {
          underlying.setHeader(k, v)
        }
      }
      if (!headers.contains(Names.CONTENT_TYPE) && request.characterEncoding.isDefined) {
        underlying.setHeader(Names.CONTENT_TYPE, "text/plain; charset="+request.characterEncoding.get)
      }
      request.cookies.responseCookies foreach { cookie => underlying.addHeader(Names.SET_COOKIE, cookie.toCookieString) }
      if (usesWriter) writer.flush()
      val content = outputStream.buffer()
      if (!chunked) underlying.setHeader(Names.CONTENT_LENGTH, content.readableBytes())
      if (HttpMessage.isKeepAlive(request) && request.serverProtocol == Http10) underlying.setHeader(Names.CONNECTION, Values.KEEP_ALIVE)
      underlying.setContent(content)
//      println("RESPONSE")
//      println(underlying)
      val fut = connection.getChannel.write(underlying)
      if(!HttpMessage.isKeepAlive(request)) fut.addListener(ChannelFutureListener.CLOSE)

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