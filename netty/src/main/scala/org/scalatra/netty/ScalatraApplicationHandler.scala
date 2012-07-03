package org.scalatra
package netty

import org.jboss.netty.channel.ChannelHandler.Sharable
import org.jboss.netty.channel.{ChannelStateEvent, MessageEvent, ChannelHandlerContext}
import store.session.InMemorySessionStore
import java.util.concurrent.atomic.AtomicBoolean

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
            app.session = current getOrElse appContext.sessions.newSession
            if (current.isEmpty) req.cookies += appContext.sessionIdKey -> app.session.id
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
