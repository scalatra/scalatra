package org.scalatra

import scala.language.experimental.macros

import scala.reflect.macros.Context

object RouteMacros {

  def rescopeAction[C <: Context](c: C)(action: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._

    object RequestTransformer extends Transformer {
      override def transform(tree: Tree): Tree = {
        tree match {
          case q"$a.this.request" => Ident(newTermName("request"))
          case q"$a.this.response" => Ident(newTermName("response"))
          case _ => super.transform(tree)
        }
      }
    }

    // return an AsyncResult
    // return a StableResult.is
    // in all other cases wrap the action in a StableResult to provide a stable lexical scope and return the res.is

    if (action.actualType <:< implicitly[TypeTag[AsyncResult]].tpe) {

      action

    } else if (action.actualType <:< implicitly[TypeTag[StableResult]].tpe) {

      c.Expr[Any](q"""$action.is""")

    } else {

      val clsName = newTypeName(c.fresh("cls"))
      val resName = newTermName(c.fresh("res"))

      val rescopedAction = q"""
          class $clsName extends org.scalatra.StableResult {
            val is = {
               $action
            }
          }
          val $resName = new $clsName()
          $resName.is
         """

      val transformedAction = c.resetLocalAttrs(RequestTransformer.transform(rescopedAction))

      c.Expr[Unit](transformedAction)

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

}