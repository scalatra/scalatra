package org.scalatra
package macroutils

import Compat210._

import scala.language.experimental.macros

/**
 * Macro implementation which generates Scalatra core DSL APIs.
 *
 * For details (e.g. moving a Tree to a new owner, 2.10 compatibility layer) see: https://github.com/scalamacros/macrology201/commits/part1
 */
object CoreDslMacros {

  type Context = blackbox.Context

  class CoreDslMacros[C <: Context](val c: C) extends MacrosCompat210 {

    import c.universe._

    /**
     * Re-scopes the expression.
     *
     * - takes an Expr[Any], wraps it in a StableResult.
     * - replaces all references to request/response to use the stable values from StableResult (instead of the ThreadLocal)
     * - returns StableResult.is
     */
    def rescopeExpression(expr: c.Expr[Any]): c.Expr[Any] = {
      import c.universe._

      val typeIsNothing = expr.actualType =:= implicitly[TypeTag[Nothing]].tpe

      if (!typeIsNothing && expr.actualType <:< implicitly[TypeTag[AsyncResult]].tpe) {
        // return an AsyncResult (for backward compatibility, AsyncResult is deprecated in 2.4)

        c.Expr[Any](q"""${splicer(expr.tree)}""")

      } else if (!typeIsNothing && expr.actualType <:< implicitly[TypeTag[StableResult]].tpe) {
        // return a StableResult.is

        c.Expr[Any](q"""${splicer(expr.tree)}.is""")

      } else {
        // in all other cases wrap the action in a StableResult to provide a stable lexical scope and return the res.is

        val clsName = c.universe.TypeName(c.freshName("cls"))
        val resName = c.universe.TermName(c.freshName("res"))

        // add to new lexical scope
        val rescopedTree =
          q"""
            class $clsName extends _root_.org.scalatra.StableResult {
              val is = {
                ${splicer(expr.tree)}
              }
            }
            val $resName = new $clsName()
            $resName.is
         """

        // typecheck the three, creates symbols (class, valdefs)
        val rescopedTreeTyped = c.typecheck(rescopedTree)

        // use stable request/response values from the new lexical scope
        val transformedTreeTyped = c.internal.typingTransform(rescopedTreeTyped)((tree, api) => tree match {
          case q"$a.this.request" => api.typecheck(Select(This(clsName), c.universe.TermName("request")))
          case q"$a.this.response" => api.typecheck(Select(This(clsName), c.universe.TermName("response")))
          case _ => api.default(tree)
        })

        c.Expr[Unit](transformedTreeTyped)

      }

    }

    def addRouteGen(method: c.Expr[HttpMethod], transformers: Seq[c.Expr[RouteTransformer]], action: c.Expr[Any]): c.Expr[Route] = {
      val rescopedAction = rescopeExpression(action)
      c.Expr[Route](q"""addRoute($method, _root_.scala.collection.immutable.Seq(..$transformers), $rescopedAction)""")
    }

    def beforeImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
      val rescopedAction = rescopeExpression(block)
      c.Expr[Unit](q"""appendBeforeFilter(_root_.scala.collection.immutable.Seq(..$transformers):_*)($rescopedAction)""")

    }

    def afterImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
      val rescopedAction = rescopeExpression(block)
      c.Expr[Unit](q"""appendAfterFilter(_root_.scala.collection.immutable.Seq(..$transformers):_*)($rescopedAction)""")
    }

    def getImpl(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(c.universe.reify(Get), transformers, action)
    }

    def postImpl(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(c.universe.reify(Post), transformers, action)
    }

    def putImpl(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(c.universe.reify(Put), transformers, action)
    }

    def deleteImpl(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(c.universe.reify(Delete), transformers, action)
    }

    def optionsImpl(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(c.universe.reify(Options), transformers, action)
    }

    def headImpl(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(c.universe.reify(Head), transformers, action)
    }

    def patchImpl(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(c.universe.reify(Patch), transformers, action)
    }

    def trapImpl(codes: c.Expr[Range])(block: c.Expr[Any]): c.Expr[Unit] = {
      val rescopedAction = rescopeExpression(block)
      c.Expr[Unit](q"""addStatusRoute($codes, $rescopedAction)""")
    }

    def trapCodeImpl(code: c.Expr[Int])(block: c.Expr[Any]): c.Expr[Unit] = {
      val rescopedAction = rescopeExpression(block)
      c.Expr[Unit](q"""addStatusRoute(_root_.scala.collection.immutable.Range($code, $code+1), $rescopedAction)""")
    }

    def notFoundImpl(block: c.Expr[Any]): c.Expr[Unit] = {
      val rescopedBlock = rescopeExpression(block)
      c.Expr[Unit](q"""setNotFoundHandler($rescopedBlock)""")
    }

    def methodNotAllowedImpl(block: c.Expr[MethodNotAllowedHandler]): c.Expr[Unit] = {
      val rescopedBlock = rescopeExpression(block)

      // defer instantiating of the StableResult by wrapping in a function
      val tree =
        q"""
          setMethodNotAllowedHandler((methods: _root_.scala.collection.immutable.Set[_root_.org.scalatra.HttpMethod]) => ${splicer(rescopedBlock.tree)}(methods))
        """

      c.Expr[Unit](tree)
    }

    def errorImpl(handler: c.Expr[ErrorHandler]): c.Expr[Unit] = {
      val rescopedHandler = rescopeExpression(handler)

      val tree =
        q"""
          addErrorHandler({
           new _root_.scala.PartialFunction[_root_.java.lang.Throwable, _root_.scala.Any]() {

             def handler = {
               ${splicer(rescopedHandler.tree)}
             }

             override def apply(v1: _root_.java.lang.Throwable): _root_.scala.Any = {
               handler.apply(v1)
             }

             override def isDefinedAt(x: _root_.java.lang.Throwable): _root_.scala.Boolean = {
               handler.isDefinedAt(x)
             }

           }
         })
        """

      c.Expr[Unit](tree)
    }

    // TODO check

    def makeAsynchronously(block: c.Expr[Any]): c.Expr[Any] = {
      val block1 = c.untypecheck(block.tree.duplicate)
      c.Expr[Any](c.typecheck(q"""asynchronously($block1)()"""))
    }

    def asyncGetImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(reify(Get), transformers, makeAsynchronously(block))
    }

    def asyncPostImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(reify(Post), transformers, makeAsynchronously(block))
    }

    def asyncPutImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(reify(Put), transformers, makeAsynchronously(block))
    }

    def asyncDeleteImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(reify(Delete), transformers, makeAsynchronously(block))
    }

    def asyncOptionsImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(reify(Options), transformers, makeAsynchronously(block))
    }

    def asyncPatchImpl(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
      addRouteGen(reify(Patch), transformers, makeAsynchronously(block))
    }

    // inspired by https://gist.github.com/retronym/10640845#file-macro2-scala
    // check out the gist for a detailed explanation of the technique
    private def splicer(tree: c.Tree): c.Tree = {
      import internal._, decorators._
      tree.updateAttachment(OrigOwnerAttachment(c.internal.enclosingOwner))
      q"_root_.org.scalatra.macroutils.Splicer.changeOwner($tree)"
    }

  }

  def beforeImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    new CoreDslMacros[c.type](c).beforeImpl(transformers: _*)(block)
  }

  def afterImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    new CoreDslMacros[c.type](c).afterImpl(transformers: _*)(block)
  }

  def getImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).getImpl(transformers: _*)(action)
  }

  def postImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).postImpl(transformers: _*)(action)
  }

  def putImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).putImpl(transformers: _*)(action)
  }

  def deleteImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).deleteImpl(transformers: _*)(action)
  }

  def optionsImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).optionsImpl(transformers: _*)(action)
  }

  def headImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).headImpl(transformers: _*)(action)
  }

  def patchImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(action: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).patchImpl(transformers: _*)(action)
  }

  def trapImpl(c: Context)(codes: c.Expr[Range])(block: c.Expr[Any]): c.Expr[Unit] = {
    new CoreDslMacros[c.type](c).trapImpl(codes)(block)
  }

  def trapCodeImpl(c: Context)(code: c.Expr[Int])(block: c.Expr[Any]): c.Expr[Unit] = {
    new CoreDslMacros[c.type](c).trapCodeImpl(code)(block)
  }

  def notFoundImpl(c: Context)(block: c.Expr[Any]): c.Expr[Unit] = {
    new CoreDslMacros[c.type](c).notFoundImpl(block)
  }

  def methodNotAllowedImpl(c: Context)(block: c.Expr[MethodNotAllowedHandler]): c.Expr[Unit] = {
    new CoreDslMacros[c.type](c).methodNotAllowedImpl(block)
  }

  def errorImpl(c: Context)(handler: c.Expr[ErrorHandler]): c.Expr[Unit] = {
    new CoreDslMacros[c.type](c).errorImpl(handler)
  }

  def asyncGetImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).asyncGetImpl(transformers: _*)(block)
  }

  def asyncPostImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).asyncPostImpl(transformers: _*)(block)
  }

  def asyncPutImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).asyncPutImpl(transformers: _*)(block)
  }

  def asyncDeleteImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).asyncDeleteImpl(transformers: _*)(block)
  }

  def asyncOptionsImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).asyncOptionsImpl(transformers: _*)(block)
  }

  def asyncPatchImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    new CoreDslMacros[c.type](c).asyncPatchImpl(transformers: _*)(block)
  }

}

