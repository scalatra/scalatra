package org.scalatra
package netty

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.frame.TooLongFrameException
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import java.io.{UnsupportedEncodingException, FileNotFoundException, RandomAccessFile, File}
import java.net.URLDecoder
import javax.activation.MimetypesFileTypeMap
import java.util.{TimeZone, Date}
import java.text.SimpleDateFormat
import io.Codec

object StaticFileHandler {

  // Many people would put a cache in but to me that is just a horrifying thought.
  // There is zero-copy so you don't incur a cost and you're not storing those
  // 2GB files in memory either. Either you store the bytes in the JVM which bloats them a bit
  // or you keep them on a store that is made to store files: the disk.
  def serveFile(ctx: ChannelHandlerContext, request: HttpRequest, file: File, contentType: Option[String] = None) = {
    try {
      val raf = new RandomAccessFile(file, "r")
      val length = raf.length()
      val resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
      setDateHeader(resp)
      setCacheHeaders(resp, file, contentType)
      val ch = ctx.getChannel
      ch.write(resp)
      val future = if (ch.getPipeline.get(classOf[SslHandler]) != null) {
        ch.write(new ChunkedFile(raf, 0, length, 8192))
      } else {
        // no ssl, zero-copy is a go
        val region = new DefaultFileRegion(raf.getChannel, 0, length)
        val fut = ch.write(region)
        fut.addListener(new ChannelFutureProgressListener {
          def operationProgressed(future: ChannelFuture, amount: Long, current: Long, total: Long) {
            printf("%s: %d / %d (+%d)%n", file.getPath, current, total, amount)
          }

          def operationComplete(future: ChannelFuture) {
            region.releaseExternalResources()
          }
        })
        fut
      }

      if (!HttpHeaders.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
    } catch {
      case _: FileNotFoundException => sendError(ctx, HttpResponseStatus.NOT_FOUND)
    }
  }

  private val mimes = new MimetypesFileTypeMap(getClass.getResourceAsStream("/mime.types"))

  def HttpDateFormat = {
    val f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
    f.setTimeZone(TimeZone.getTimeZone("GMT"))
    f
  }

  def HttpNow = HttpDateFormat.format(new Date)
  val HttpCacheSeconds = 60
  private def setDateHeader(response: HttpResponse) {
    response.setHeader(HttpHeaders.Names.DATE, HttpNow)
  }

  private def setCacheHeaders(response: HttpResponse, fileToCache: File, contentType: Option[String]) {
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType getOrElse mimes.getContentType(fileToCache))
    response.setHeader(HttpHeaders.Names.EXPIRES, HttpNow)
    response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HttpCacheSeconds)
    response.setHeader(HttpHeaders.Names.LAST_MODIFIED, HttpDateFormat.format(new Date(fileToCache.lastModified())))
    HttpHeaders.setContentLength(response, fileToCache.length())
  }

  def sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
    response.setContent(ChannelBuffers.copiedBuffer("Failure: " + status.toString + "\r\n", Codec.UTF8))

    // Close the connection as soon as the error message is sent.
    ctx.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }

  def sendNotModified(ctx: ChannelHandlerContext) {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED)
    response.setHeader(HttpHeaders.Names.DATE, HttpNow)

    // Close the connection as soon as the error message is sent.
    ctx.getChannel.write(response).addListener(ChannelFutureListener.CLOSE)
  }
}

class StaticFileHandler(publicDirectory: String) extends SimpleChannelUpstreamHandler {

  private def isModified(request: HttpRequest, file: File) = {
    val ifModifiedSince = request.getHeader(HttpHeaders.Names.IF_MODIFIED_SINCE)
    if (ifModifiedSince.nonBlank) {
      val date = StaticFileHandler.HttpDateFormat.parse(ifModifiedSince)
      val ifModifiedDateSeconds = date.getTime / 1000
      val fileLastModifiedSeconds = file.lastModified() / 1000
      ifModifiedDateSeconds == fileLastModifiedSeconds
    } else false
  }

  @throws(classOf[Exception])
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case request: HttpRequest if request.getMethod != HttpMethod.GET =>
        StaticFileHandler.sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED)
      case request: HttpRequest => {
        val path = sanitizeUri(request.getUri)
        if (path == null) {
          StaticFileHandler.sendError(ctx, HttpResponseStatus.FORBIDDEN)
        } else {
          val file = new File(path)
          if (file.isHidden || !file.exists()) StaticFileHandler.sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED)
          else if (!file.isFile) StaticFileHandler.sendError(ctx, HttpResponseStatus.FORBIDDEN)
          else {
            if (isModified(request, file)) {
              StaticFileHandler.sendNotModified(ctx)
            } else {
              StaticFileHandler.serveFile(ctx, request, file)
            }
          }
        }
      }
    }
  }

  @throws(classOf[Exception])
  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getCause match {
      case ex: TooLongFrameException => StaticFileHandler.sendError(ctx, HttpResponseStatus.BAD_REQUEST)
      case ex =>
        ex.printStackTrace()
        if (e.getChannel.isConnected) StaticFileHandler.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR)
    }
  }

  private def sanitizeUri(uri: String) = {
    // Decode the path.
    val decoded = try {
      URLDecoder.decode(uri, CharsetUtil.UTF_8.displayName())
    } catch {
      case _: UnsupportedEncodingException =>
        URLDecoder.decode(uri, CharsetUtil.ISO_8859_1.displayName())
    }

    // Convert file separators.
    val converted = decoded.replace('/', File.separatorChar)

    val uf = new File(sys.props("user.dir") + File.separatorChar + publicDirectory).getAbsolutePath
    val pth = uf + File.separatorChar + converted
    val f = new File(pth)
    val absPath = f.getAbsolutePath
    if (!(absPath startsWith uf)) null
    else absPath
  }


}