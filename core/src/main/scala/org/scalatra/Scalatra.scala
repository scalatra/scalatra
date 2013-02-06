package org.scalatra

import scala.language.experimental.macros

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import scala.reflect.macros._

import org.{http4s => h4s}
import org.http4s._
import play.api.libs.iteratee.{Enumerator, Done}

import util.MultiMap
import scala.util.parsing.combinator.testing.Ident

trait Scalatra
  extends h4s.Route
  with HandlerImplicits
{
  private var router: h4s.Route = PartialFunction.empty

  protected[scalatra] def request: Request = macro ScalatraMacros.request

  private[scalatra] def DummyRequest: Request =
    throw new AssertionError("Should have been rewritten by macro. This is a bug.")

  private var _request: Request = _

  def apply(req: Request): h4s.Handler = applyOrElse(req, PartialFunction.empty)

  override def applyOrElse[A1 <: Request, B1 >: h4s.Handler](req: A1, default: (A1) => B1): B1 =
    router.applyOrElse(req, default)

  def isDefinedAt(req: Request): Boolean = router.isDefinedAt(req)

  // Is a macro so we can rewrite it as a function of the context, i.e., this.
  def get(matcher: RequestMatcher)(f: h4s.Handler) = macro ScalatraMacros.get

  protected implicit def stringToRequestMatcher(path: String): RequestMatcher = {
    req: Request =>
      if (req.pathInfo == path)
        Some(MultiMap.empty)
      else
        None
  }

  protected[scalatra] def addRoute(method: Method, matchers: RequestMatcher*)(f: Request => h4s.Handler) {
    val route = new Scalatra.Route {
      def apply(req: Request): h4s.Handler = applyOrElse(req, PartialFunction.empty)
      override def applyOrElse[A1 <: Request, B1 >: h4s.Handler](req: A1, default: (A1) => B1): B1 =
        if (isDefinedAt(req)) f(req) else default(req)
      def isDefinedAt(req: Request): Boolean =
        matchers.forall(_(req).isDefined)
    }
    router = router orElse route
  }
}

object Scalatra {
  type Route = h4s.Route
}

trait HandlerImplicits {
  implicit def stringToHandler(s: String): h4s.Handler = Done(Responder(body = Enumerator(s.getBytes)))
}

object ScalatraMacros {
  type ScalatraContext = Context { type PrefixType = Scalatra }

  def request(c: ScalatraContext): c.Expr[Request] = {
    if (c.enclosingMethod != null) {
      c.error(c.macroApplication.pos, "invalid request access")
    }
    c.universe.reify { c.prefix.splice.DummyRequest }
  }

  def get(c: ScalatraContext)(matcher: c.Expr[RequestMatcher])(f: c.Expr[h4s.Handler]): c.Expr[Unit] = {
    import c.universe._

    val Request = newTermName("DummyRequest")
    def rewrite(tree: Tree) = new Transformer {
      override def transform(tree: Tree): Tree =
        tree match {
          case Select(tree, Request) => c.universe.Ident("req")
          case tree => super.transform(tree)
        }
    }.transform(tree)

    c.universe.reify {
      c.prefix.splice.addRoute(Method.Get, matcher.splice) {
        implicit req: Request => c.Expr[org.http4s.Handler](c.resetLocalAttrs(rewrite(f.tree))).splice
      }
    }
  }
}