package org.scalatra

import scala.language.experimental.macros

import MacrosCompat.{ Context, freshName, typeName, termName, typecheck, untypecheck }

object CoreDslMacros {

  // takes an Expr[Any], wraps it in a StableResult
  // replaces all references to request/response to use the stable values from StableResult (instead of the ThreadLocal)
  // returns StableResult.is
  def rescopeExpression[C <: Context](c: C)(expr: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._

    // return an AsyncResult
    // return a StableResult.is
    // in all other cases wrap the action in a StableResult to provide a stable lexical scope and return the res.is

    if (expr.actualType <:< implicitly[TypeTag[AsyncResult]].tpe) {

      expr

    } else if (expr.actualType <:< implicitly[TypeTag[StableResult]].tpe) {

      c.Expr[Any](q"""$expr.is""")

    } else {

      val clsName = typeName[c.type](c)(freshName(c)("cls"))
      val resName = termName[c.type](c)(freshName(c)("res"))

      object BendRequestResponse extends Transformer {
        override def transform(tree: Tree): Tree = {
          tree match {
            case q"$a.this.request" => Select(This(clsName), termName[c.type](c)("request"))
            case q"$a.this.response" => Select(This(clsName), termName[c.type](c)("response"))
            case _ => super.transform(tree)
          }
        }
      }

      // duplicate and untype the tree
      val untypedExpr = untypecheck[c.type](c)(expr.tree.duplicate)

      // add to new lexical scope
      val rescopedExpr = q"""
          class $clsName extends org.scalatra.StableResult {
            val is = {
               $untypedExpr
            }
          }
          val $resName = new $clsName()
          $resName.is
         """

      // use stable request/response values from the new lexical scope
      val transformedExpr = BendRequestResponse.transform(rescopedExpr)

      // println(show(transformedAction))

      c.Expr[Unit](transformedExpr)

    }

  }

  def addRouteGen[C <: Context](c: C)(method: c.Expr[HttpMethod], transformers: Seq[c.Expr[RouteTransformer]], action: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._

    val rescopedAction = rescopeExpression[c.type](c)(action)
    c.Expr[Route](q"""addRoute($method, Seq(..$transformers), $rescopedAction)""")
  }

  def beforeImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeExpression[c.type](c)(block)
    c.Expr[Unit](q"""routes.appendBeforeFilter(Route(Seq(..$transformers), () => $rescopedAction))""")
  }

  def afterImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeExpression[c.type](c)(block)
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

    val rescopedAction = rescopeExpression[c.type](c)(block)
    c.Expr[Unit](q"""addStatusRoute($codes, $rescopedAction)""")
  }

  def trapCodeImpl(c: Context)(code: c.Expr[Int])(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeExpression[c.type](c)(block)
    c.Expr[Unit](q"""addStatusRoute(Range($code, $code+1), $rescopedAction)""")
  }

  def notFoundImpl(c: Context)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val tree = q"""
      doNotFound = {
        () => ${rescopeExpression[c.type](c)(block)}
      }
    """

    c.Expr[Unit](tree)
  }

  def methodNotAllowedImpl(c: Context)(block: c.Expr[Set[HttpMethod] => Any]): c.Expr[Unit] = {
    import c.universe._

    val tree =
      q"""
         doMethodNotAllowed = (methods: Set[HttpMethod]) => ${rescopeExpression[c.type](c)(block)}(methods)
        """

    // println(show(tree))

    c.Expr[Unit](tree)
  }

  def errorImpl(c: Context)(handler: c.Expr[ErrorHandler]): c.Expr[Unit] = {
    import c.universe._

    val tree =
      q"""errorHandler = {
           new PartialFunction[Throwable, Any]() {

             def handler = {
               ${rescopeExpression[c.type](c)(handler)}
             }

             override def apply(v1: Throwable): Any = {
               handler.apply(v1)
             }

             override def isDefinedAt(x: Throwable): Boolean = {
               handler.isDefinedAt(x)
             }

           }
         } orElse errorHandler"""

    // println(show(tree))

    c.Expr[Unit](tree)
  }

  def makeAsynchronously[C <: Context](c: C)(block: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._
    val block1 = untypecheck[c.type](c)(block.tree.duplicate)
    c.Expr[Any](typecheck[c.type](c)(q"""asynchronously($block1)()"""))
  }

  def asyncGetImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    addRouteGen[c.type](c)(reify(Get), transformers, makeAsynchronously[c.type](c)(block))
  }

  def asyncPostImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    addRouteGen[c.type](c)(reify(Post), transformers, makeAsynchronously[c.type](c)(block))
  }

  def asyncPutImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    addRouteGen[c.type](c)(reify(Put), transformers, makeAsynchronously[c.type](c)(block))
  }

  def asyncDeleteImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    addRouteGen[c.type](c)(reify(Delete), transformers, makeAsynchronously[c.type](c)(block))
  }

  def asyncOptionsImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    addRouteGen[c.type](c)(reify(Options), transformers, makeAsynchronously[c.type](c)(block))
  }

  def asyncPatchImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    addRouteGen[c.type](c)(reify(Patch), transformers, makeAsynchronously[c.type](c)(block))
  }

}