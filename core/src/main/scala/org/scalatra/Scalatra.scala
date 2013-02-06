package org.scalatra

import scala.language.experimental.macros

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import scala.reflect.macros._

import org.{http4s => h4s}
import org.http4s._
import play.api.libs.iteratee.{Enumerator, Done}

import util.MultiMap

abstract class Scalatra // has to be a class to be cloneable
  extends Cloneable
  with h4s.Route
  with HandlerImplicits
  with Scalatra.Context
{
  // TODO This can't be an http4s route, because we use ourself as the context.
  // It could probably be redone to look a lot like the old route registry.
  private var router: Scalatra.Route = PartialFunction.empty

  protected[scalatra] def request: Request = _request
  private var _request: Request = _

  def apply(req: Request): h4s.Handler = applyOrElse(req, PartialFunction.empty)

  override def applyOrElse[A1 <: Request, B1 >: h4s.Handler](req: A1, default: (A1) => B1): B1 = {
    val klone = cloneWithRequest(req)
    klone.router.applyOrElse(klone, { _: Scalatra => default(req) })
  }

  def isDefinedAt(req: Request): Boolean = {
    val klone = cloneWithRequest(req)
    klone.router.isDefinedAt(klone)
  }

  private[scalatra] def cloneWithRequest(req: Request) = {
    val klone = this.clone().asInstanceOf[Scalatra]
    klone._request = req
    klone
  }

  // Is a macro so we can rewrite it as a function of the context, i.e., this.
  def get(matcher: RequestMatcher)(f: h4s.Handler) = macro ScalatraMacros.get

  protected implicit def stringToRequestMatcher(path: String): RequestMatcher = {
    req: Request =>
      if (req.pathInfo == path)
        Some(MultiMap.empty)
      else
        None
  }

  protected[scalatra] def addRoute(method: Method, matchers: RequestMatcher*)(f: Scalatra.Context => h4s.Handler) {
    val route = new Scalatra.Route {
      def apply(ctx: Scalatra.Context): h4s.Handler = applyOrElse(ctx, PartialFunction.empty)
      override def applyOrElse[A1 <: Scalatra.Context, B1 >: h4s.Handler](ctx: A1, default: (A1) => B1): B1 =
        if (isDefinedAt(ctx)) f(ctx) else default(ctx)
      def isDefinedAt(ctx: Scalatra.Context): Boolean =
        matchers.forall(_(ctx.request).isDefined)
    }
    router = router orElse route
  }
}

object Scalatra {
  type Route = PartialFunction[Scalatra.Context, h4s.Handler]

  trait Context {
    protected[scalatra] def request: Request
  }
}

trait HandlerImplicits {
  implicit def stringToHandler(s: String): h4s.Handler = Done(Responder(body = Enumerator(s.getBytes)))
}

object ScalatraMacros {
  type ScalatraContext = Context { type PrefixType = Scalatra }

  def get(c: ScalatraContext)(matcher: c.Expr[RequestMatcher])(f: c.Expr[h4s.Handler]): c.Expr[Unit] = {
    import c.universe._

    val Request = newTermName("request")
    def rewrite(tree: Tree) = new Transformer {
      override def transform(tree: Tree): Tree =
        tree match {
          case Select(_, Request) =>
            // TODO verify that we're selecting from a Scalatra.
            Select(Ident(newTermName("ctx")), Request)
          case tree =>
            super.transform(tree)
        }
    }.transform(tree)

    c.universe.reify {
      c.prefix.splice.addRoute(Method.Get, matcher.splice) {
        ctx: Scalatra.Context => c.Expr[org.http4s.Handler](c.resetLocalAttrs(rewrite(f.tree))).splice
      }
    }
  }
}