package org.scalatra

import scala.language.experimental.macros

import scala.concurrent.Future
import scala.reflect.macros.blackbox.Context

import org.scalatra._

object RouteMacros {

  def rescopeAction[C <: Context](c: C)(action: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._

    if (action.actualType <:< c.mirror.typeOf[Future[_]]) {

      val rescopedAction = q"""
          new org.scalatra.AsyncResult {
            val is = {
               $action
             }
           }
         """

      c.Expr[Unit](rescopedAction)
    } else {
      action
    }

  }

  def addRouteGen[C <: Context](c: C)(method: c.Expr[HttpMethod], transformers: Seq[c.Expr[RouteTransformer]], action: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(action)
    c.Expr[Route](q"""addRoute($method, Seq(..$transformers), $rescopedAction)""")
  }

  def getImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    addRouteGen[c.type](c)(c.universe.reify(Get), transformers, action)
  }

  def postImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    addRouteGen[c.type](c)(c.universe.reify(Post), transformers, action)
  }

  def putImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    addRouteGen[c.type](c)(c.universe.reify(Put), transformers, action)
  }

  def deleteImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    addRouteGen[c.type](c)(c.universe.reify(Delete), transformers, action)
  }

  def optionsImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    addRouteGen[c.type](c)(c.universe.reify(Options), transformers, action)
  }

  def headImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    addRouteGen[c.type](c)(c.universe.reify(Head), transformers, action)
  }

  def patchImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    addRouteGen[c.type](c)(c.universe.reify(Patch), transformers, action)
  }

  def trapImpl(c: Context)(codes: c.Expr[Range])(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(block)
    c.Expr[Unit](q"""addStatusRoute($codes, $rescopedAction)""")
  }

  def get(transformers: RouteTransformer*)(action: => Any): Any = macro getImpl

  def post(transformers: RouteTransformer*)(action: => Any): Route = macro postImpl

  def put(transformers: RouteTransformer*)(action: => Any): Route = macro putImpl

  def delete(transformers: RouteTransformer*)(action: => Any): Route = macro deleteImpl

  def trap(codes: Range)(block: => Any): Unit = macro trapImpl

  def options(transformers: RouteTransformer*)(action: => Any): Route = macro optionsImpl

  def head(transformers: RouteTransformer*)(action: => Any): Route = macro headImpl

  def patch(transformers: RouteTransformer*)(action: => Any): Route = macro patchImpl

}