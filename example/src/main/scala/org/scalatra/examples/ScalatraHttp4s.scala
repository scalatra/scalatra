package org.scalatra.examples

import org.scalatra._
import org.http4s._
import org.http4s.servlet._
import scala.concurrent.Await
import scala.concurrent.duration._
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}

object ScalatraExample extends Scalatra {
  get("/foo") { request.pathInfo }
  get("/bar") { "bar" }

  // Rightfully fails to compile.
  // def pathInfo = request.pathInfo

  def pathInfo(implicit request: Request) = request.pathInfo
}

object ScalatraExampleApp extends App {
  val server = new Server(8080)

  val context = new ServletContextHandler()
  context.setContextPath("/")
  server.setHandler(context)

  val servlet = new Http4sServlet(ScalatraExample)

  context.addServlet(new ServletHolder(servlet), "/*")

  server.start()
  server.join()
}
