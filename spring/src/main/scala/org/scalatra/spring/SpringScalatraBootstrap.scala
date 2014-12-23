package org.scalatra.spring

import javax.annotation.PostConstruct
import javax.servlet.ServletContext

import org.scalatra.ScalatraServlet
import org.scalatra.servlet.RichServletContext
import org.springframework.context.{ ApplicationContext, ApplicationContextAware }
import org.springframework.stereotype.Component
import org.springframework.web.context.ServletContextAware

import scala.collection.JavaConverters._

/** @author Stephen Samuel */
@Component
class SpringScalatraBootstrap extends ApplicationContextAware with ServletContextAware {

  @PostConstruct
  def bootstrap() {

    val richContext = new RichServletContext(servletContext)
    val resources = appContext.getBeansWithAnnotation(classOf[Path])
    resources.values().asScala.foreach {
      case servlet: ScalatraServlet =>
        var path = servlet.getClass.getAnnotation(classOf[Path]).value()
        if (!path.startsWith("/")) path = "/" + path
        richContext.mount(servlet, path)
      case _ =>
    }
  }

  var servletContext: ServletContext = _
  var appContext: ApplicationContext = _
  def setServletContext(servletContext: ServletContext): Unit = this.servletContext = servletContext
  def setApplicationContext(appContext: ApplicationContext): Unit = this.appContext = appContext
}
