package org.scalatra.examples

import org.scalatra._
import org.http4s._
import scala.concurrent.Await
import scala.concurrent.duration._

object ScalatraExample extends Scalatra {
  get("/foo") { request.pathInfo }
  get("/bar") { "bar" }
}

object ScalatraExampleApp extends App {
  import scala.concurrent.ExecutionContext.Implicits.global

  val server = new MockServer(ScalatraExample)

  def render(response: MockServer.Response) {
    println(response.statusLine)
    response.headers.foreach(println)
    println
    System.out.write(response.body)
    println
    println
  }

  for (path <- Seq("/foo", "/bar", "/baz")) {
    println(s"Requesting $path")
    render(Await.result(server(Request(pathInfo = path)), 3 seconds))
  }
}
