package org.scalatra.example

import org.scalatra._
import org.scalatra.jetty.JettyServer

object EmbeddedApp extends ScalatraServlet {
  get("/") { <h1>Hello, world!</h1> }
}

object EmbeddedServer {
  def main(args: Array[String]) {
    new JettyServer()
      .mount(EmbeddedApp, "/*")
      .start()
      .join()
  }
}
