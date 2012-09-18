package org.scalatra
package atmosphere

import org.atmosphere.cpr._
import org.atmosphere.util.ExcludeSessionBroadcaster
import java.util
import util.concurrent.Future
import javax.servlet.http.HttpSession
import collection.JavaConverters._
import grizzled.slf4j.Logger
import org.atmosphere.cpr.DefaultBroadcaster.Entry


class ScalatraBroadcaster(id: String, config: AtmosphereConfig, wireFormat: WireFormat) extends DefaultBroadcaster(id, config) {
  private[this] val logger: Logger = Logger[ScalatraBroadcaster]

  def broadcast(msg: OutboundMessage, clientFilter: ClientFilter): Future[AnyRef] = {
    val selectedResources = resources.asScala filter clientFilter
    addBroadcasterListener(new BroadcasterListener {
      def onPreDestroy(b: Broadcaster) {

      }

      def onPostCreate(b: Broadcaster) {

      }

      def onComplete(b: Broadcaster) {

      }
    })
    broadcast(wireFormat.render(msg), selectedResources.toSet.asJava)
  }
}