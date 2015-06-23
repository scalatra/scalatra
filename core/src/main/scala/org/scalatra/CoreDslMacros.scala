package org.scalatra

import scala.language.experimental.macros

import MacrosCompat.{ Context, freshName, typeName, termName, typecheck, untypecheck }

/**
 * Macro implementation which generates Scalatra core DSL APIs.
 */
object CoreDslMacros {

  /**
   * Re-scopes the expression.
   *
   * - takes an Expr[Any], wraps it in a StableResult.
   * - replaces all references to request/response to use the stable values from StableResult (instead of the ThreadLocal)
   * - returns StableResult.is
   */
  def rescopeExpression[C <: Context](c: C)(expr: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._
    import c.internal._

    val typeIsNothing = expr.actualType =:= implicitly[TypeTag[Nothing]].tpe

    if (!typeIsNothing && expr.actualType <:< implicitly[TypeTag[AsyncResult]].tpe) {
      // return an AsyncResult (for backward compatibility, AsyncResult is deprecated in 2.4)

      c.Expr[Any](q"""${splicer[c.type](c)(expr.tree)}""")

    } else if (!typeIsNothing && expr.actualType <:< implicitly[TypeTag[StableResult]].tpe) {
      // return a StableResult.is

      c.Expr[Any](q"""${splicer[c.type](c)(expr.tree)}.is""")

    } else {
      // in all other cases wrap the action in a StableResult to provide a stable lexical scope and return the res.is

      val clsName = typeName[c.type](c)(freshName(c)("cls"))
      val resName = termName[c.type](c)(freshName(c)("res"))

      // add to new lexical scope
      val rescopedTree =
        q"""
            class $clsName extends _root_.org.scalatra.StableResult {
              val is = {
                ${splicer[c.type](c)(expr.tree)}
              }
            }
            val $resName = new $clsName()
            $resName.is
         """

      // typecheck the three, creates symbols (class, valdefs)
      val rescopedTreeTyped = c.typecheck(rescopedTree)  // this is not the owner

      // use stable request/response values from the new lexical scope
      val transformedTreeTyped = c.internal.typingTransform(rescopedTreeTyped)((tree, api) => tree match {
        case q"$a.this.request" => api.typecheck(Select(This(clsName), TermName("request")))
        case q"$a.this.response" => api.typecheck(Select(This(clsName), TermName("response")))
        case _ => api.default(tree)
      })

      c.Expr[Unit](transformedTreeTyped)

    }

  }

  // inspired by https://gist.github.com/retronym/10640845#file-macro2-scala
  // check out the gist for a detailed explanation of the technique
  private def splicer[C <: Context](c: C)(tree: c.Tree): c.Tree = {
    import c.universe._, c.internal._, decorators._
    tree.updateAttachment(OrigOwnerAttachment(enclosingOwner))
    q"_root_.org.scalatra.CoreDslMacros.Splicer.changeOwner($tree)"
  }

  case class OrigOwnerAttachment(sym: Any)
  object Splicer {
    def impl(c: Context)(tree: c.Tree): c.Tree = {
      import c.universe._, c.internal._, decorators._
      val origOwner = tree.attachments.get[OrigOwnerAttachment].map(_.sym).get.asInstanceOf[Symbol]
      c.internal.changeOwner(tree, origOwner, c.internal.enclosingOwner)
    }
    def changeOwner[A](tree: A): A = macro impl
  }

  def addRouteGen[C <: Context](c: C)(method: c.Expr[HttpMethod], transformers: Seq[c.Expr[RouteTransformer]], action: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._

    val rescopedAction = rescopeExpression[c.type](c)(action)
    c.Expr[Route](q"""addRoute($method, _root_.scala.collection.immutable.Seq(..$transformers), $rescopedAction)""")
  }

  def beforeImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeExpression[c.type](c)(block)
    c.Expr[Unit](q"""routes.appendBeforeFilter(_root_.org.scalatra.Route(Seq(..$transformers), () => ${splicer[c.type](c)(rescopedAction.tree)}))""")

  }

  def afterImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedAction = rescopeExpression[c.type](c)(block)
    c.Expr[Unit](q"""routes.appendAfterFilter(_root_.org.scalatra.Route(Seq(..$transformers), () => ${splicer[c.type](c)(rescopedAction.tree)}))""")
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
    c.Expr[Unit](q"""addStatusRoute(scala.collection.immutable.Range($code, $code+1), $rescopedAction)""")
  }

  def notFoundImpl(c: Context)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedBlock = rescopeExpression[c.type](c)(block)

    val tree = q"""
      doNotFound = {
        () => ${splicer[c.type](c)(rescopedBlock.tree)}
      }
    """

    c.Expr[Unit](tree)
  }

  def methodNotAllowedImpl(c: Context)(block: c.Expr[Set[HttpMethod] => Any]): c.Expr[Unit] = {
    import c.universe._

    val rescopedBlock = rescopeExpression[c.type](c)(block)

    val tree =
      q"""
          doMethodNotAllowed = (methods: _root_.scala.collection.immutable.Set[_root_.org.scalatra.HttpMethod]) => ${splicer[c.type](c)(rescopedBlock.tree)}(methods)
        """

    c.Expr[Unit](tree)
  }

  def errorImpl(c: Context)(handler: c.Expr[ErrorHandler]): c.Expr[Unit] = {
    import c.universe._

    val rescopedHandler = rescopeExpression[c.type](c)(handler)

    val tree =
      q"""
          errorHandler = {
           new _root_.scala.PartialFunction[_root_.java.lang.Throwable, _root_.scala.Any]() {

             def handler = {
               ${splicer[c.type](c)(rescopedHandler.tree)}
             }

             override def apply(v1: _root_.java.lang.Throwable): _root_.scala.Any = {
               handler.apply(v1)
             }

             override def isDefinedAt(x: _root_.java.lang.Throwable): _root_.scala.Boolean = {
               handler.isDefinedAt(x)
             }

           }
         } orElse errorHandler
        """

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