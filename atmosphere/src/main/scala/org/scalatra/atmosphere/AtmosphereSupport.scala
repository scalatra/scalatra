package org.scalatra
package atmosphere

import java.io.IOException
import java.util
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import javax.servlet.{ FilterConfig, ServletConfig, ServletContext, ServletException }

import _root_.akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger
import org.apache.catalina.CometProcessor
import org.atmosphere.cache.UUIDBroadcasterCache
import org.atmosphere.client.TrackMessageSizeInterceptor
import org.atmosphere.container.{ JBossWebCometSupport, Tomcat7CometSupport, TomcatCometSupport }
import org.atmosphere.cpr._
import org.atmosphere.interceptor.SessionCreationInterceptor
import org.jboss.servlet.http.{ HttpEvent, HttpEventServlet }
import org.json4s._
import org.scalatra.json.JsonSupport
import org.scalatra.servlet.ScalatraAsyncSupport
import org.scalatra.util.RicherString._

import scala.collection.JavaConverters._
import scala.util.control.Exception.allCatch

trait AtmosphereSupport extends Initializable with Handler with CometProcessor with HttpEventServlet with org.apache.catalina.comet.CometProcessor with ScalatraAsyncSupport { self: ScalatraBase with org.scalatra.SessionSupport with JsonSupport[_] =>

  private[this] val logger = Logger[this.type]

  private[this] val _defaultWireformat = new JacksonSimpleWireformat

  /**
   * Override this to use another ScalaBroadcaster like RedisScalatraBroadcaster
   *
   * Example: RedisScalatraBroadcasterConfig(URI.create("redis://127.0.0.1"), Some("password"))
   */
  protected val broadcasterConfig: BroadcasterConf = ScalatraBroadcasterConfig(classOf[DefaultScalatraBroadcaster])

  implicit protected def wireFormat: WireFormat = _defaultWireformat

  implicit def json2JsonMessage(json: JValue): OutboundMessage = JsonMessage(json)

  implicit def string2Outbound(text: String): OutboundMessage = text.blankOption map { txt =>
    if (txt.startsWith("{") || txt.startsWith("["))
      parseOpt(txt) map JsonMessage.apply getOrElse TextMessage(txt)
    else
      TextMessage(txt)
  } getOrElse TextMessage("")

  private[this] def isFilter = self match {
    case _: ScalatraFilter => true
    case _ => false
  }

  val atmosphereFramework = new ScalatraAtmosphereFramework(isFilter, false)

  implicit protected def scalatraActorSystem: ActorSystem =
    servletContext.get(ActorSystemKey).map(_.asInstanceOf[ActorSystem]) getOrElse {
      val msg = "Scalatra Actor system not present. Creating a private actor system"
      logger.info(msg)
      val cfg = ConfigFactory.load
      val defRef = ConfigFactory.defaultReference
      ActorSystem("scalatra", cfg.getConfig("scalatra").withFallback(defRef))
    }

  private[this] implicit def filterConfig2servletConfig(fc: FilterConfig): ServletConfig = {
    new ServletConfig {
      def getInitParameter(name: String): String = getServletContext.getInitParameter(name)
      def getInitParameterNames() = getServletContext.getInitParameterNames()
      def getServletName() = fc.getFilterName()
      def getServletContext() = fc.getServletContext()
    }
  }

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    val cfg: ServletConfig = config match {
      case c: FilterConfig => c
      case c: ServletConfig => new ServletConfig {
        def getInitParameterNames: util.Enumeration[String] = getServletContext.getInitParameterNames
        def getServletName: String = c.getServletName
        def getInitParameter(name: String): String = getServletContext.getInitParameter(name)
        def getServletContext: ServletContext = c.getServletContext
      }
    }

    allCatch.withApply(ex => logger.error(ex.getMessage, ex)) {
      atmosphereFramework.enableSessionSupport()
      configureBroadcasterCache()
      configureBroadcasterFactory()
      configureInterceptors(cfg)
      atmosphereFramework.init(cfg)
      setupAtmosphereHandlerMappings(cfg)
    }
  }

  /**
   * Servlets that want to track atmosphere message size should override this.
   * @see [[TrackMessageSizeInterceptor]]
   */
  protected def trackMessageSize: Boolean = false

  protected def configureInterceptors(cfg: ServletConfig) = {
    atmosphereFramework.interceptor(new SessionCreationInterceptor)
    if (cfg.getInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT).isBlank)
      cfg.getServletContext.setInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true")
    if (trackMessageSize || cfg.getInitParameter(TrackMessageSize).blankOption.map(_.toCheckboxBool).getOrElse(false))
      atmosphereFramework.interceptor(new TrackMessageSizeInterceptor)
  }

  private[this] def setupAtmosphereHandlerMappings(cfg: ServletConfig) {
    // TODO: also support filters?
    val servletRegistration = ScalatraBase.getServletRegistration(this)
    servletRegistration foreach { reg =>
      reg.getMappings.asScala foreach { mapping =>
        atmosphereFramework.addAtmosphereHandler(mapping, new ScalatraAtmosphereHandler).initAtmosphereHandler(cfg)
      }
    }
  }

  /**
   * Handles a request and renders a response.
   *
   * $ 1. If the request lacks a character encoding, `defaultCharacterEncoding`
   * is set to the request.
   *
   * $ 2. Sets the response's character encoding to `defaultCharacterEncoding`.
   *
   * $ 3. Binds the current `request`, `response`, and `multiParams`, and calls
   * `executeRoutes()`.
   */
  abstract override def handle(request: HttpServletRequest, response: HttpServletResponse) {
    withRequestResponse(request, response) {
      val atmoRoute = atmosphereRoute(request)
      if (atmoRoute.isDefined) {
        request(AtmosphereRouteKey) = atmoRoute.get
        request.getSession(true) // force session creation
        if (request.get(FrameworkConfig.ATMOSPHERE_HANDLER_WRAPPER).isEmpty)
          atmosphereFramework.doCometSupport(AtmosphereRequest.wrap(request), AtmosphereResponse.wrap(response))
      } else {
        super.handle(request, response)
      }
    }
  }

  private[this] def noGetRoute = sys.error("You are using the AtmosphereSupport without defining any Get route," +
    "you should get rid of it.")

  private[this] def atmosphereRoutes = routes.methodRoutes.getOrElse(Get, noGetRoute).filter(_.metadata.contains('Atmosphere))

  private[this] def atmosphereRoute(req: HttpServletRequest) = (for {
    route <- atmosphereRoutes.toStream
    matched <- route(requestPath)
  } yield matched).headOption

  private[this] def configureBroadcasterFactory() {
    val factory = new ScalatraBroadcasterFactory(
      atmosphereFramework.getAtmosphereConfig,
      broadcasterConfig)
    atmosphereFramework.setDefaultBroadcasterClassName(broadcasterConfig.broadcasterClass.getName)
    atmosphereFramework.setBroadcasterFactory(factory)
  }

  private[this] def configureBroadcasterCache() {
    if (atmosphereFramework.getBroadcasterCacheClassName.isBlank)
      atmosphereFramework.setBroadcasterCacheClassName(classOf[UUIDBroadcasterCache].getName)
  }

  private[atmosphere] val Atmosphere: RouteTransformer = { (route: Route) =>
    route.copy(metadata = route.metadata + ('Atmosphere -> 'Atmosphere))
  }

  def atmosphere(transformers: RouteTransformer*)(block: => AtmosphereClient) = {
    val newTransformers = transformers :+ Atmosphere
    get(newTransformers: _*)(block)
    post(newTransformers: _*) { () }
  }

  /**
   * Hack to support Tomcat AIO like other WebServer. This method is invoked
   * by Tomcat when it detect a [[javax.servlet.Servlet]] implements the interface
   * [[org.apache.catalina.CometProcessor]] without invoking [[javax.servlet.Servlet#service]]
   *
   * @param cometEvent the [[org.apache.catalina.CometEvent]]
   * @throws java.io.IOException
   * @throws javax.servlet.ServletException
   */
  @throws(classOf[IOException])
  @throws(classOf[ServletException])
  def event(cometEvent: org.apache.catalina.CometEvent) {
    val req = cometEvent.getHttpServletRequest
    val res = cometEvent.getHttpServletResponse
    req.setAttribute(TomcatCometSupport.COMET_EVENT, cometEvent)

    atmosphereFramework.setupTomcat()
    handle(req, res)

    val transport = cometEvent.getHttpServletRequest.getParameter(HeaderConfig.X_ATMOSPHERE_TRANSPORT)
    if (transport != null && transport.equalsIgnoreCase(HeaderConfig.WEBSOCKET_TRANSPORT)) {
      cometEvent.close()
    }
  }

  /**
   * Hack to support Tomcat 7 AIO
   */
  @throws(classOf[IOException])
  @throws(classOf[ServletException])
  def event(cometEvent: org.apache.catalina.comet.CometEvent) {
    val req = cometEvent.getHttpServletRequest
    val res = cometEvent.getHttpServletResponse
    req.setAttribute(Tomcat7CometSupport.COMET_EVENT, cometEvent)

    atmosphereFramework.setupTomcat7()
    handle(req, res)

    val transport = cometEvent.getHttpServletRequest.getParameter(HeaderConfig.X_ATMOSPHERE_TRANSPORT)
    if (transport != null && transport.equalsIgnoreCase(HeaderConfig.WEBSOCKET_TRANSPORT)) {
      cometEvent.close()
    }
  }

  /**
   * Hack to support JBossWeb AIO like other WebServer. This method is invoked
   * by Tomcat when it detect a [[javax.servlet.Servlet]] implements the interface
   * [[org.jboss.servlet.http.HttpEventServlet]] without invoking [[javax.servlet.Servlet#service]]
   *
   * @param httpEvent the [[org.jboss.servlet.http.HttpEvent]]
   * @throws java.io.IOException
   * @throws javax.servlet.ServletException
   */
  @throws(classOf[IOException])
  @throws(classOf[ServletException])
  def event(httpEvent: HttpEvent) {
    val req = httpEvent.getHttpServletRequest
    val res = httpEvent.getHttpServletResponse
    req.setAttribute(JBossWebCometSupport.HTTP_EVENT, httpEvent)

    atmosphereFramework.setupJBoss()
    handle(req, res)
  }
}
