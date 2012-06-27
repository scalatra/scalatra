package org.scalatra
package netty

import scala.io.Codec
import org.jboss.netty.channel.{ExceptionEvent, ChannelHandlerContext, ChannelFutureListener, SimpleChannelUpstreamHandler}
import org.jboss.netty.handler.codec.http2.{HttpResponseStatus, HttpVersion, DefaultHttpResponse}
import org.jboss.netty.buffer.ChannelBuffers
import scala.util.control.Exception.ignoring

abstract class ScalatraUpstreamHandler extends SimpleChannelUpstreamHandler with ScalatraLogging {

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    try {
      logger error ("Caught an exception", e.getCause)
      val resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR)
      resp.setContent(ChannelBuffers.copiedBuffer((e.getCause.getMessage + "\n" + e.getCause.getStackTraceString).getBytes(Codec.UTF8)))
      ctx.getChannel.write(resp).addListener(ChannelFutureListener.CLOSE)
    } catch {
      case e => {
        logger error ("Error during error handling", e)
        ignoring(classOf[Throwable]) { ctx.getChannel.close().await() }
      }
    }
  }
}