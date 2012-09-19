package org.scalatra
package atmosphere

import org.atmosphere.handler.AbstractReflectorAtmosphereHandler
import org.atmosphere.cpr._
import org.scalatra.servlet.ServletApiImplicits._
import org.json4s.Formats
import java.nio.CharBuffer
import org.scalatra.util.RicherString._
import grizzled.slf4j.Logger

object ScalatraAtmosphereHandler {
  val AtmosphereClientKey = "org.scalatra.atmosphere.AtmosphereClientConnection"
}
class ScalatraAtmosphereHandler(app: ScalatraBase with SessionSupport)(implicit formats: Formats) extends AbstractReflectorAtmosphereHandler {
  import ScalatraAtmosphereHandler._
//  private[this] val logger: Logger = Logger(getClass)

  private val wireFormat: WireFormat = new SimpleJsonWireFormat

  def onRequest(resource: AtmosphereResource) {
    println("Handling on request %s".format(resource.getRequest.getRequestURI))
    val req = resource.getRequest
    val method = req.requestMethod
    println("The request method: %s" format method)
    val route = atmosphereRoute(req)
    println("The route: %s" format req)
    val isAtmosphereRequest = route.isDefined

    if (isAtmosphereRequest) {
      addAtmosphereRequestAttributes(resource)
      val isNew = req.getSession.contains(AtmosphereClientKey)
      val client: AtmosphereClient  = clientFromSessionOrFactory(route.get)

      if (method == Post) {
        println("This is an incoming message")
        handleIncomingMessage(req, client)
      } else {
        if (isNew) {
          println("This is a new client: %s".format(isNew))
          client.resource = resource
          println("Linked client to atmosphere resource")
          resumeIfNeeded(resource)
          println("Configured resume")
          configureBroadcaster(resource)
          println("Created the broadcaster")
          client.receive.lift(Connected)
          println("Notified of connected")
        }
        resource.getResponse.write("OK\n")

        resource.suspend()
        println("resource suspended")
      }
    } else {
      println("regular scalatra request")
//      app.withRequestResponse()
      app.handle(req.wrappedRequest(), resource.getResponse)
    }
  }


  override def onStateChange(event: AtmosphereResourceEvent) {
    println("State changed for [%s]" format event.getResource.getRequest.getRequestURI)
    super.onStateChange(event)
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

  private[this] lazy val atmosphereRoutes = app.routes.methodRoutes(Get).filter(_.metadata.contains('Atmosphere))

  private[this] def atmosphereRoute(req: AtmosphereRequest) = (for {
    route <- atmosphereRoutes.toStream
    matched <- route()
  } yield matched).headOption

  private[this] def resumeIfNeeded(resource: AtmosphereResource) {
    import AtmosphereResource.TRANSPORT._
    resource.transport match {
      case JSONP | AJAX | LONG_POLLING =>
        println("Request will resume on broadcast.")
        resource.resumeOnBroadcast(true)
      case _ =>
    }
  }

  def destroy() {}
}
