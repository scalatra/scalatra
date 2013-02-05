package org.scalatra

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import org.{http4s => h4s}
import org.http4s._
import org.http4s.Bodies._
import play.api.libs.iteratee.{Enumerator, Done}

import util.MultiMap
import java.util.concurrent.Executors

trait Scalatra extends h4s.Route with HandlerImplicits {
  private var router: h4s.Route = PartialFunction.empty

  def apply(req: Request): h4s.Handler = applyOrElse(req, PartialFunction.empty)

  override def applyOrElse[A1 <: Request, B1 >: h4s.Handler](req: A1, default: (A1) => B1): B1 =
    router.applyOrElse(req, default)

  def isDefinedAt(req: Request): Boolean = router.isDefinedAt(req)

  def get(path: String)(f: => h4s.Handler): h4s.Route = addRoute(Method.Get, pathToRequestMatcher(path))(f)

  private def pathToRequestMatcher(path: String): RequestMatcher = {
    req: Request => if (req.pathInfo == path) Some(MultiMap.empty) else None
  }

  protected def addRoute(method: Method, matchers: RequestMatcher*)(f: => h4s.Handler): h4s.Route = {
    val route = new h4s.Route {
      def apply(req: Request): h4s.Handler = applyOrElse(req, PartialFunction.empty)
      override def applyOrElse[A1 <: Request, B1 >: h4s.Handler](req: A1, default: (A1) => B1): B1 =
        if (isDefinedAt(req)) f else default(req)
      def isDefinedAt(req: Request): Boolean =
        matchers.forall(_(req).isDefined)
    }
    router = router orElse route
    route
  }
}

trait HandlerImplicits {
  implicit def stringToHandler(s: String): h4s.Handler = Done(Responder(body = Enumerator(s.getBytes)))
}

object ScalatraExample extends Scalatra {
  get("/foo") { "foo" }
  get("/bar") { "bar" }
}

object ScalatraExampleApp extends App {
  import ExecutionContext.Implicits.global

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
