package org.scalatra
package atmosphere

import org.atmosphere.handler.AbstractReflectorAtmosphereHandler
import org.atmosphere.cpr._
import org.scalatra.servlet.ServletApiImplicits._
import org.json4s.Formats
import java.nio.CharBuffer
import org.scalatra.util.RicherString._

object ScalatraAtmosphereHandler {
  val AtmosphereClientKey = "org.scalatra.atmosphere.AtmosphereClientConnection"
}
class ScalatraAtmosphereHandler(app: ScalatraBase with SessionSupport)(implicit formats: Formats) extends AbstractReflectorAtmosphereHandler {
  import ScalatraAtmosphereHandler._

  private val wireFormat: WireFormat = new SimpleJsonWireFormat

  def onRequest(resource: AtmosphereResource) {
    val req = resource.getRequest
    app.withRequestResponse(req, resource.getResponse) {
      val method = req.requestMethod
      val route = atmosphereRoute(req)
      val session = resource.session()
      val isAtmosphereRequest = route.isDefined

      if (isAtmosphereRequest) {
        addAtmosphereRequestAttributes(resource)
        val isNew = !session.contains(AtmosphereClientKey)

        if (method == Post) {
          val client: AtmosphereClient  = session(AtmosphereClientKey).asInstanceOf[AtmosphereClient]
          handleIncomingMessage(req, client)
        } else {
          if (isNew) {

            val client = clientForRoute(route.get)
            session(AtmosphereClientKey) = client
            client.resource = resource
            resource.addEventListener(new AtmosphereResourceEventListener {
              def onThrowable(event: AtmosphereResourceEvent) {
                client.receive.lift(Error(Option(event.throwable())))
              }

              def onBroadcast(event: AtmosphereResourceEvent) {}

              def onSuspend(event: AtmosphereResourceEvent) {}

              def onDisconnect(event: AtmosphereResourceEvent) {
                client.receive.lift(Disconnected(Option(event.throwable)))
              }

              def onResume(event: AtmosphereResourceEvent) {}
            })
            resumeIfNeeded(resource)
            configureBroadcaster(resource)
            client.receive.lift(Connected)
          }

          resource.getResponse.write("OK\n")
          resource.suspend()
        }
      } else {
        app.handle(req.wrappedRequest(), resource.getResponse)
      }
    }
  }


  private[this] def clientForRoute(route: MatchedRoute): AtmosphereClient = {
    liftAction(route.action) getOrElse {
      throw new ScalatraException("An atmosphere route should return an atmosphere client")
    }
  }

  private[this] def addAtmosphereRequestAttributes(resource: AtmosphereResource) = {
    resource.getRequest.setAttribute(FrameworkConfig.ATMOSPHERE_RESOURCE, resource)
    resource.getRequest.setAttribute(FrameworkConfig.ATMOSPHERE_HANDLER, this)
  }

  private[this] def requestUri(resource: AtmosphereResource) = {
    val u = resource.getRequest.getRequestURI.blankOption getOrElse "/"
    if (u.endsWith("/")) u + "*" else u + "/*"
  }

  private[this] def configureBroadcaster(resource: AtmosphereResource) {
    val bc = BroadcasterFactory.getDefault.get(requestUri(resource))
    resource.setBroadcaster(bc)
    resource.getRequest.setAttribute(AtmosphereResourceImpl.SKIP_BROADCASTER_CREATION, true)
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

  private[this] lazy val atmosphereRoutes = app.routes.methodRoutes(Get).filter(_.metadata.contains('Atmosphere))

  private[this] def atmosphereRoute(req: AtmosphereRequest) = (for {
    route <- atmosphereRoutes.toStream
    matched <- route(ScalatraServlet.requestPath(req))
  } yield matched).headOption

  private[this] def resumeIfNeeded(resource: AtmosphereResource) {
    import AtmosphereResource.TRANSPORT._
    resource.transport match {
      case JSONP | AJAX | LONG_POLLING => resource.resumeOnBroadcast(true)
      case _ =>
    }
  }

  def destroy() {}
}
