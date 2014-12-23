package org.scalatra.example

import org.scalatra.jetty.JettyServer

object EmbeddedServer {
  def main(args: Array[String]) {
    new JettyServer()
      .start()
      .join()
  }
}
