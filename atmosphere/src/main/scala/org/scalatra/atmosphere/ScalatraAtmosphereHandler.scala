package org.scalatra
package atmosphere

import org.atmosphere.handler.AbstractReflectorAtmosphereHandler
import org.atmosphere.cpr._
import org.scalatra.servlet.ServletApiImplicits._
import org.json4s.Formats
import java.nio.CharBuffer
import org.scalatra.util.RicherString._
import javax.servlet.http.HttpSession
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT._

object ScalatraAtmosphereHandler {
  val AtmosphereClientKey = "org.scalatra.atmosphere.AtmosphereClientConnection"
  val AtmosphereRouteKey = "org.scalatra.atmosphere.AtmosphereRoute"

  private class ScalatraResourceEventListener extends AtmosphereResourceEventListener {
    def client(resource: AtmosphereResource) =
      Option(resource.session()).flatMap(_.get(AtmosphereClientKey)).map(_.asInstanceOf[AtmosphereClient])

    def onBroadcast(event: AtmosphereResourceEvent) {
      val resource = event.getResource
        resource.transport match {
          case JSONP | AJAX | LONG_POLLING =>
          case _ => resource.getResponse().flushBuffer()
        }
    }

    def onDisconnect(event: AtmosphereResourceEvent) {
      val disconnector = if (event.isCancelled) ClientDisconnected else ServerDisconnected
      client(event.getResource) foreach (_.receive.lift(Disconnected(disconnector, Option(event.throwable))))
    }

    def onResume(event: AtmosphereResourceEvent) {}

    def onSuspend(event: AtmosphereResourceEvent) {}

    def onThrowable(event: AtmosphereResourceEvent) {
      client(event.getResource) foreach (_.receive.lift(Error(Option(event.throwable()))))
    }
  }
}
class ScalatraAtmosphereHandler(implicit formats: Formats, wireFormat: WireFormat) extends AbstractReflectorAtmosphereHandler {
  import ScalatraAtmosphereHandler._

  def onRequest(resource: AtmosphereResource) {
    val req = resource.getRequest
    val method = req.requestMethod
    val route = Option(req.getAttribute(AtmosphereRouteKey)).map(_.asInstanceOf[MatchedRoute])
    val session = resource.session()
    val isNew = !session.contains(AtmosphereClientKey)

    if (method == Post) {
      val client = session(AtmosphereClientKey).asInstanceOf[AtmosphereClient]
      handleIncomingMessage(req, client)
    } else {
      val client = createClient(route.get, session, resource)
      addEventListener(resource)
      resumeIfNeeded(resource)
      configureBroadcaster(resource)
      //client.receive.lift(Connected)

      resource.suspend

    }
  }

  private[this] def createClient(route: MatchedRoute, session: HttpSession, resource: AtmosphereResource) = {
    val client = clientForRoute(route)
    session(AtmosphereClientKey) = client
    client.resource = resource
    client
  }

  private[this] def clientForRoute(route: MatchedRoute): AtmosphereClient = {
    liftAction(route.action) getOrElse {
      throw new ScalatraException("An atmosphere route should return an atmosphere client")
    }
  }

  private[this] def requestUri(resource: AtmosphereResource) = {
    val u = resource.getRequest.getRequestURI.blankOption getOrElse "/"
    if (u.endsWith("/")) u + "*" else u + "/*"
  }

  private[this] def configureBroadcaster(resource: AtmosphereResource) {
    val bc = BroadcasterFactory.getDefault.get(requestUri(resource))
    resource.setBroadcaster(bc)
  }

  private[this] def handleIncomingMessage(req: AtmosphereRequest, client: AtmosphereClient) {
    val parsed: InboundMessage = wireFormat.parseInMessage(readBody(req))
    client.receive.lift(parsed)
  }

  private[this] def readBody(req: AtmosphereRequest) = {
    val buff = CharBuffer.allocate(8192)
    val body = new StringBuilder
    val rdr = req.getReader
    while (rdr.read(buff) >= 0) {
      body.append(buff.flip.toString)
      buff.clear()
    }
    body.toString()
  }

  private[this] def  addEventListener(resource: AtmosphereResource) {
    resource.addEventListener(new ScalatraResourceEventListener)
  }

  private[this] def liftAction(action: org.scalatra.Action) = try {
    action() match {
      case cl: AtmosphereClient => Some(cl)
      case _ => None
    }
  } catch {
    case t: Throwable =>
      t.printStackTrace()
      None
  }

  private[this] def resumeIfNeeded(resource: AtmosphereResource) {
    import AtmosphereResource.TRANSPORT._
    resource.transport match {
      case JSONP | AJAX | LONG_POLLING => resource.resumeOnBroadcast(true)
      case _ =>
    }
  }

  def destroy() {}
}
