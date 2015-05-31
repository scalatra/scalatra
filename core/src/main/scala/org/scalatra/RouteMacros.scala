package org.scalatra

import scala.language.experimental.macros

import scala.reflect.macros.Context

object RouteMacros {

  def rescopeAction[C <: Context](c: C)(action: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._

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

      object RequestTransformer extends Transformer {
        override def transform(tree: Tree): Tree = {
          tree match {
            case q"$a.this.request" => Select(This(clsName), newTermName("request"))
            case q"$a.this.response" => Select(This(clsName), newTermName("response"))
            case _ => super.transform(tree)
          }
        }
      }

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

      // println(show(transformedAction))

      c.Expr[Unit](transformedAction)

    }

  }

  def addRouteGen[C <: Context](c: C)(method: c.Expr[HttpMethod], transformers: Seq[c.Expr[RouteTransformer]], action: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(action)
    c.Expr[Route](q"""addRoute($method, Seq(..$transformers), $rescopedAction)""")
  }

  def beforeImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(block)
    c.Expr[Unit](q"""routes.appendBeforeFilter(Route(Seq(..$transformers), () => $rescopedAction))""")
  }

  def afterImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(block)
    c.Expr[Unit](q"""routes.appendAfterFilter(Route(Seq(..$transformers), () => $rescopedAction))""")
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

  def trapCodeImpl(c: Context)(code: c.Expr[Int])(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(block)
    c.Expr[Unit](q"""addStatusRoute(Range($code, $code+1), $rescopedAction)""")
  }

  def notFoundImpl(c: Context)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(block)

    val tree = q"""
      doNotFound = {
        () => $rescopedAction
      }
    """

    c.Expr[Unit](tree)
  }

  def methodNotAllowedImpl(c: Context)(block: c.Expr[Set[HttpMethod] => Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(block)

    val tree =
      q"""
         doMethodNotAllowed = (methods: Set[HttpMethod]) => $rescopedAction(methods)
        """

    println(show(tree))

    c.Expr[Unit](tree)
  }

  def errorImpl(c: Context)(handler: c.Expr[ErrorHandler]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeAction[c.type](c)(handler)

    val tree =
      q"""errorHandler = {
           new PartialFunction[Throwable, Any]() {

             def handler = {
               $rescopedAction
             }

             override def apply(v1: Throwable): Any = {
               handler.apply(v1)
             }

             override def isDefinedAt(x: Throwable): Boolean = {
               handler.isDefinedAt(x)
             }

           }
         } orElse errorHandler"""

    //    println(show(tree))

    c.Expr[Unit](tree)
  }

}