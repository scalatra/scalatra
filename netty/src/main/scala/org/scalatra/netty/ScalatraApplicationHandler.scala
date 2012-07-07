package org.scalatra
package netty

import org.jboss.netty.channel.ChannelHandler.Sharable
import org.jboss.netty.channel.{ChannelStateEvent, MessageEvent, ChannelHandlerContext}

/**
 * This handler is shared across the entire server, providing application level settings
 */
@Sharable
class ScalatraApplicationHandler(implicit val appContext: AppContext) extends ScalatraUpstreamHandler {

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case req: NettyHttpRequest => {
        appContext.application(req) match {
          case Some(app: ScalatraApp with SessionSupport) => {
            val current = req.cookies get appContext.sessionIdKey flatMap appContext.sessions.get
            val sess = current getOrElse appContext.sessions.newSession
            req(SessionSupport.SessionKey) = sess
            if (current.isEmpty) req.cookies += appContext.sessionIdKey -> sess.id
          }
          case _ =>
        }
        ctx.sendUpstream(e)
      }
      case _ => {
        super.messageReceived(ctx, e)
      }
    }
  }

  def stop() = {
    appContext.sessions.stop()
  }

}
