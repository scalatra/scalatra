package org.scalatra
package atmosphere

import java.nio.CharBuffer
import javax.servlet.http.{ HttpServletRequest, HttpSession }

import grizzled.slf4j.Logger
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT._
import org.atmosphere.cpr._
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler
import org.scalatra.servlet.ServletApiImplicits._
import org.scalatra.util.RicherString._

object ScalatraAtmosphereHandler {
  @deprecated("Use `org.scalatra.atmosphere.AtmosphereClientKey` instead", "2.2.1")
  val AtmosphereClientKey = org.scalatra.atmosphere.AtmosphereClientKey
  @deprecated("Use `org.scalatra.atmosphere.AtmosphereRouteKey` instead", "2.2.1")
  val AtmosphereRouteKey = org.scalatra.atmosphere.AtmosphereRouteKey

  private class ScalatraResourceEventListener extends AtmosphereResourceEventListener {
    def client(resource: AtmosphereResource) =
      Option(resource.session()).flatMap(_.get(org.scalatra.atmosphere.AtmosphereClientKey)).map(_.asInstanceOf[AtmosphereClient])

    def onPreSuspend(event: AtmosphereResourceEvent) {}

    def onBroadcast(event: AtmosphereResourceEvent) {
      val resource = event.getResource
      resource.transport match {
        case JSONP | AJAX | LONG_POLLING =>
        case _ => resource.getResponse.flushBuffer()
      }
    }

    def onDisconnect(event: AtmosphereResourceEvent) {
      val disconnector = if (event.isCancelled) ClientDisconnected else ServerDisconnected
      client(event.getResource) foreach (_.receive.lift(Disconnected(disconnector, Option(event.throwable))))
      //      if (!event.getResource.isResumed) {
      //        event.getResource.session.invalidate()
      //      } else {
      event.getResource.session.removeAttribute(org.scalatra.atmosphere.AtmosphereClientKey)
      //      }
    }

    def onResume(event: AtmosphereResourceEvent) {}

    def onSuspend(event: AtmosphereResourceEvent) {}

    def onThrowable(event: AtmosphereResourceEvent) {
      client(event.getResource) foreach (_.receive.lift(Error(Option(event.throwable()))))
    }

    def onClose(event: AtmosphereResourceEvent) {}
  }
}

class ScalatraAtmosphereException(message: String) extends ScalatraException(message)
class ScalatraAtmosphereHandler(implicit wireFormat: WireFormat) extends AbstractReflectorAtmosphereHandler {
  import org.scalatra.atmosphere.ScalatraAtmosphereHandler._

  private[this] val internalLogger = Logger(getClass)

  def onRequest(resource: AtmosphereResource) {
    val req = resource.getRequest
    val route = Option(req.getAttribute(org.scalatra.atmosphere.AtmosphereRouteKey)).map(_.asInstanceOf[MatchedRoute])
    var session = resource.session()
    val isNew = !session.contains(org.scalatra.atmosphere.AtmosphereClientKey)

    (req.requestMethod, route.isDefined) match {
      case (Post, _) =>
        var client: AtmosphereClient = null
        if (isNew) {
          session = AtmosphereResourceFactory.getDefault.find(resource.uuid).session
        }

        client = session(org.scalatra.atmosphere.AtmosphereClientKey).asInstanceOf[AtmosphereClient]
        handleIncomingMessage(req, client)
      case (_, true) =>
        val cl = if (isNew) {
          createClient(route.get, session, resource)
        } else null

        addEventListener(resource)
        resumeIfNeeded(resource)
        configureBroadcaster(resource)
        if (isNew && cl != null) cl.receive.lift(Connected)
        resource.suspend
      case _ =>
        val ex = new ScalatraAtmosphereException("There is no atmosphere route defined for " + req.getRequestURI)
        internalLogger.warn(ex.getMessage)
        throw ex
    }
  }

  private[this] def createClient(route: MatchedRoute, session: HttpSession, resource: AtmosphereResource) = {
    withRouteMultiParams(route, resource.getRequest) {
      val client = clientForRoute(route)
      session(org.scalatra.atmosphere.AtmosphereClientKey) = client
      client.resource = resource
      client
    }
  }
  private[this] def createClient(route: MatchedRoute, resource: AtmosphereResource) = {
    withRouteMultiParams(route, resource.getRequest) {
      val client = clientForRoute(route)
      client.resource = resource
      client
    }
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

  private[this] def addEventListener(resource: AtmosphereResource) {
    resource.addEventListener(new ScalatraResourceEventListener)
  }
  /**
   * The current multiparams.  Multiparams are a result of merging the
   * standard request params (query string or post params) with the route
   * parameters extracted from the route matchers of the current route.
   * The default value for an unknown param is the empty sequence.  Invalid
   * outside `handle`.
   */
  private[this] def multiParams(request: HttpServletRequest): MultiParams = {
    val read = request.contains("MultiParamsRead")
    val found = request.get(MultiParamsKey) map (
      _.asInstanceOf[MultiParams] ++ (if (read) Map.empty else request.multiParameters)
    )
    val multi = found getOrElse request.multiParameters
    request("MultiParamsRead") = new {}
    request(MultiParamsKey) = multi
    multi.withDefaultValue(Seq.empty)
  }
  private[this] def withRouteMultiParams[S](matchedRoute: MatchedRoute, request: HttpServletRequest)(thunk: => S): S = {
    val originalParams = multiParams(request)
    setMultiparams(matchedRoute, originalParams, request)
    try {
      thunk
    } finally {
      request(MultiParamsKey) = originalParams
    }
  }

  def setMultiparams[S](matchedRoute: MatchedRoute, originalParams: MultiParams, request: HttpServletRequest) {
    val routeParams = matchedRoute.multiParams map {
      case (key, values) =>
        key -> values.map(UriDecoder.secondStep(_))
    }
    request(MultiParamsKey) = originalParams ++ routeParams
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
    import org.atmosphere.cpr.AtmosphereResource.TRANSPORT._
    resource.transport match {
      case JSONP | AJAX | LONG_POLLING => resource.resumeOnBroadcast(true)
      case _ =>
    }
  }
}
