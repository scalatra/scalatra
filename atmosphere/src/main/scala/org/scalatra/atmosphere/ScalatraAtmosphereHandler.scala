package org.scalatra
package atmosphere

import org.atmosphere.handler.AbstractReflectorAtmosphereHandler
import org.atmosphere.cpr._
import org.scalatra.servlet.ServletApiImplicits._
import org.json4s.Formats
import java.nio.CharBuffer

object ScalatraAtmosphereHandler {
  val AtmosphereClientKey = "org.scalatra.atmosphere.AtmosphereClientConnection"
}
class ScalatraAtmosphereHandler(app: ScalatraBase with SessionSupport)(implicit formats: Formats) extends AbstractReflectorAtmosphereHandler {
  import ScalatraAtmosphereHandler._

  private val wireFormat: WireFormat = new SimpleJsonWireFormat

  def onRequest(resource: AtmosphereResource) {
    val req = resource.getRequest
    val method = HttpMethod(req.getMethod)
    val route = atmosphereRoute(req)
    val isAtmosphereRequest = route.isDefined

    if (isAtmosphereRequest) {
      req.setAttribute(FrameworkConfig.ATMOSPHERE_RESOURCE, resource)
      req.setAttribute(FrameworkConfig.ATMOSPHERE_HANDLER, this)

      val client: AtmosphereClient  = clientFromSessionOrFactory(route.get)
      if (method == Post) {
        handleIncomingMessage(req, client)
      } else {
        client.resource = resource
        resumeIfNeeded(resource)
        client.receive.lift(Connected)
        resource.suspend()
      }
    } else {
      app.handle(req.wrappedRequest(), resource.getResponse)
    }
  }

  private[this] def handleIncomingMessage(req: AtmosphereRequest, client: AtmosphereClient) {
    val parsed: InboundMessage = wireFormat.parseInMessage(readBody(req))
    client.receive.lift(parsed)
  }

  private[this] def clientFromSessionOrFactory(route: MatchedRoute): AtmosphereClient = {
    val client = app.session.get(AtmosphereClientKey).map(_.asInstanceOf[AtmosphereClient]) getOrElse {
      liftAction(route.action) getOrElse {
        throw new ScalatraException("An atmosphere route should return an atmosphere client")
      }
    }
    app.session(AtmosphereClientKey) = client
    client
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

  private[this] def liftAction(act: org.scalatra.Action) = try {
    act.apply() match {
      case cl: AtmosphereClient => Some(cl)
      case _ => None
    }
  } catch {
    case _: Throwable => None
  }

  lazy val atmosphereRoutes = app.routes.methodRoutes(Get).filter(_.metadata.contains('Atmosphere))

  def atmosphereRoute(req: AtmosphereRequest) = (for {
    route <- atmosphereRoutes.toStream
    matched <- route()
  } yield matched).headOption

  def resumeIfNeeded(resource: AtmosphereResource) {
    import AtmosphereResource.TRANSPORT._
    resource.transport match {
      case JSONP | AJAX | LONG_POLLING => resource.resumeOnBroadcast(true)
      case _ =>
    }
  }

  def destroy() {}
}
