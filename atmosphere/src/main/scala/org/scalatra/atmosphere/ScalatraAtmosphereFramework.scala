package org.scalatra
package atmosphere

import org.atmosphere.cpr.{Action => AtmoAction, BroadcasterFactory, AtmosphereResponse, AtmosphereRequest, AtmosphereFramework}
import org.atmosphere.container._
import java.util.UUID
import org.atmosphere.cpr.ApplicationConfig._
import org.atmosphere.cpr.FrameworkConfig._
import org.atmosphere.cpr.HeaderConfig._
import org.atmosphere.websocket.WebSocket._
import org.scalatra.servlet.ServletApiImplicits._
import scala.util.control.Exception._
import org.scalatra.util.RicherString._
import grizzled.slf4j.Logger
import scala.Some


class ScalatraAtmosphereFramework(isFilter: Boolean = false, autoDetectHandlers: Boolean = false) extends AtmosphereFramework(isFilter, autoDetectHandlers) {

  private[this] val logger = Logger[ScalatraAtmosphereFramework]
  def setupTomcat7() {
    if (!getAsyncSupport.supportWebSocket) {
      if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
        asyncSupport.synchronized {
          asyncSupport = new Tomcat7CometSupport(config)
        }
      }
    }
  }

  def setupTomcat() {
    if (!getAsyncSupport.supportWebSocket) {
      if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
        asyncSupport.synchronized {
          asyncSupport = new TomcatCometSupport(config)
        }
      }
    }
  }

  def setupJBoss() {
    if (!isCometSupportSpecified && !isCometSupportConfigured.getAndSet(true)) {
      asyncSupport.synchronized {
        asyncSupport = new JBossWebCometSupport(config)
      }
    }
  }

  def enableSessionSupport() = sessionSupport(true)
  def disableSessionSupport() = sessionSupport(false)

  /**
   * Invoke the proprietary {@link AsyncSupport}
   *
   * @param req
   * @param res
   * @return an [[org.atmosphere.cpr.Action]]
   * @throws IOException
   * @throws ServletException
   */
  override def doCometSupport(req: AtmosphereRequest, res: AtmosphereResponse): AtmoAction = {
    req(BROADCASTER_FACTORY) = BroadcasterFactory.getDefault
    req(PROPERTY_USE_STREAM) = useStreamForFlushingComments
    req(BROADCASTER_CLASS) = broadcasterClassName
    req(ATMOSPHERE_CONFIG) = config
    notifySetupListeners(req, res) {
      catching(classOf[IllegalStateException]).withApply(setupTomcatSupport(req,res)) {
        var skip = true
        val s = config.getInitParameter(ALLOW_QUERYSTRING_AS_REQUEST)
        if (s != null) skip = s.toBoolean
        if (!skip || req.get(WEBSOCKET_SUSPEND).isEmpty) {
          val headers = configureQueryStringAsRequest(req)
          val body = Option(headers.remove(ATMOSPHERE_POST_BODY)).flatMap(_.blankOption)
          body foreach { bd =>
            req.headers(headers).method(if (req.requestMethod == Get) "POST" else req.getMethod).body(bd)
          }
        }

        val trackId = req.getHeader(X_ATMOSPHERE_TRACKING_ID).blankOption match {
          case None | Some("0")  =>
            val theId = UUID.randomUUID().toString
            res.setHeader(X_ATMOSPHERE_TRACKING_ID, theId)
            theId

          case Some(theId) =>
            if (req.resource() == null || req.getAttribute(WEBSOCKET_INITIATED) == null)
              res.setHeader(X_ATMOSPHERE_TRACKING_ID, theId)
            theId
        }

        if (req.getAttribute(SUSPENDED_ATMOSPHERE_RESOURCE_UUID) == null)
          req.setAttribute(SUSPENDED_ATMOSPHERE_RESOURCE_UUID, trackId)
        asyncSupport.service(req, res)
      }
    }

  }

  private[this] def setupTomcatSupport(req: AtmosphereRequest, res: AtmosphereResponse)(t: Throwable): AtmoAction = {
    val ex = t.asInstanceOf[IllegalStateException]
    if (ex.getMessage != null && (ex.getMessage.startsWith("Tomcat failed") || ex.getMessage.startsWith("JBoss failed"))) {
      if (!isFilter)
        logger.warn("Failed using comet support: %s, error: %s. Is the NIO or APR connector enabled?".format(asyncSupport.getClass.getName, ex.getMessage))
      logger.debug(ex.getMessage, ex)

      asyncSupport = if (asyncSupport.supportWebSocket()) new Tomcat7BIOSupportWithWebSocket(config) else new BlockingIOCometSupport(config)
      asyncSupport.init(config.getServletConfig)

      logger.warn("Using " + asyncSupport.getClass.getName)
      asyncSupport.service(req, res)
    } else {
      logger.error("ScalataAtmosphereFramework exception", ex)
      throw ex
    }
  }

  private[this] def notifySetupListeners(req: AtmosphereRequest, res: AtmosphereResponse)(action: => AtmoAction) = {
    val a = action
    if (a != null) {
      notify(a.`type`(), req, res)
    }
    if (req != null && a != null && a.`type`() != AtmoAction.TYPE.SUSPEND) {
      req.destroy()
      res.destroy()
      notify(AtmoAction.TYPE.DESTROYED, req, res)
    }
    a
  }


}