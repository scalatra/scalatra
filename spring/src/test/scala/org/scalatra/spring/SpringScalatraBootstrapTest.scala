package org.scalatra.spring

import java.util
import javax.servlet.http.HttpServlet
import javax.servlet.{ ServletContext, ServletRegistration }

import org.mockito.{ Matchers, Mockito }
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FunSuite, OneInstancePerTest }
import org.scalatra.ScalatraServlet
import org.springframework.context.ApplicationContext

/** @author Stephen Samuel */
class SpringScalatraBootstrapTest extends FunSuite with OneInstancePerTest with MockitoSugar {

  val applicationContext = mock[ApplicationContext]
  val servletContext = mock[ServletContext]
  val bootstrapper = new SpringScalatraBootstrap
  bootstrapper.setServletContext(servletContext)
  bootstrapper.setApplicationContext(applicationContext)

  val reg = mock[ServletRegistration.Dynamic]
  Mockito.when(servletContext.addServlet(Matchers.anyString, Matchers.any[HttpServlet])).thenReturn(reg)

  val resource = new CustomerResource

  val beans = new util.HashMap[String, AnyRef]
  beans.put("resource", resource)

  Mockito.when(applicationContext.getBeansWithAnnotation(classOf[Path])).thenReturn(beans)

  test("resource beans are registered with the servlet context") {
    bootstrapper.bootstrap()
    Mockito.verify(servletContext).addServlet(classOf[CustomerResource].getName, resource)
  }

  test("resource beans use mapping from @Path") {
    bootstrapper.bootstrap()
    Mockito.verify(reg).addMapping("/customer/*")
  }
}

@Path("customer")
class CustomerResource extends ScalatraServlet

