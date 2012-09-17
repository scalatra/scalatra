package org.scalatra
package atmosphere

import json.JsonSupport
import javax.servlet.ServletConfig
import javax.servlet.FilterConfig
import org.apache.catalina.CometProcessor
import org.jboss.servlet.http.HttpEventServlet
import org.atmosphere.di.ServletContextProvider
import java.io.IOException
import javax.servlet.ServletException
import org.atmosphere.container.Tomcat7CometSupport
import org.atmosphere.container.TomcatCometSupport
import org.jboss.servlet.http.HttpEvent
import org.atmosphere.container.JBossWebCometSupport
import org.atmosphere.cpr._
import collection.JavaConverters._
import org.atmosphere.util.ExcludeSessionBroadcaster
import org.json4s.Formats

trait AtmosphereSupport extends Initializable with CometProcessor with HttpEventServlet with ServletContextProvider with org.apache.catalina.comet.CometProcessor { self: ScalatraBase with SessionSupport with JsonSupport[_] =>


  implicit protected def jsonFormats: Formats
  private[this] def isFilter = self match {
    case _: ScalatraFilter => true
    case _ => false
  }

  val atmosphereFramework = new ScalatraAtmosphereFramework(isFilter, false)

  private[this] implicit def filterConfig2servletConfig(fc: FilterConfig): ServletConfig = {
    new ServletConfig {
      def getInitParameter(name: String): String = fc.getInitParameter(name)
      def getInitParameterNames() = fc.getInitParameterNames()
      def getServletName() = fc.getFilterName()
      def getServletContext() = fc.getServletContext()
    }
  }

  abstract override def initialize(config: ConfigT) {
    val cfg: ServletConfig = config match {
      case c: FilterConfig => c
      case c: ServletConfig => c
    }
    atmosphereFramework.init(cfg)
    val never = BroadcasterLifeCyclePolicy.NEVER
    // TODO: also support filters?
    val servletRegistration = ScalatraBase.getServletRegistration(this)
    servletRegistration foreach { reg =>
      reg.getMappings.asScala foreach { mapping =>
        atmosphereFramework.addAtmosphereHandler(mapping, new ScalatraAtmosphereHandler(this)).initAtmosphereHandler(cfg)
      }
    }
  }

  private[atmosphere] val Atmosphere: RouteTransformer = route => route.copy(metadata = route.metadata + ('Atmosphere -> 'Atmosphere))

  def atmosphere(transformers: RouteTransformer*)(block: => AtmosphereClient) = {
    val newTransformers = transformers :+ Atmosphere
    get(newTransformers:_*)(block)
    post(newTransformers:_*){()}
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
