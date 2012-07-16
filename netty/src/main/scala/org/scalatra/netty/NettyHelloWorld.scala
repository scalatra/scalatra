package org.scalatra
package netty

object NettyHelloWorld extends App {

  NettyServer(6543) {
    new ScalatraApp {
      get("/") {
        "Hello, world!"
      }
    }
  }.start()
}
