package org.scalatra.macros

import scala.reflect.macros.Context
import scala.language.experimental.macros
import org.scalatra._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import scala.reflect.internal.annotations.compileTimeOnly

/**
 * @author Bryce Anderson
 *         Created on 2/23/14
 */
object DSLMacros {

  type DSLContext = Context { type PrefixType = CoreDsl }

  private[scalatra] def reqRespRewriter(c: Context)(block: c.Expr[Any]): c.Tree = {
    import c.universe._

    val reqName = newTermName("request")
    val respName = newTermName("response")

    val transformer = new Transformer {
      override def transform(tree: Tree): Tree = tree match {
        case q"$a.this.request" if tree.tpe =:= c.weakTypeOf[HttpServletRequest] => Ident(reqName)
        case q"$a.this.response" if tree.tpe =:= c.weakTypeOf[HttpServletResponse] => Ident(respName)
        case t => super.transform(t)
      }
    }

    c.resetLocalAttrs(transformer.transform(block.tree))
  }

  private def actionBuilder(c: DSLContext)(block: c.Expr[Any]): c.Expr[Action] = {
    import c.universe._

    val newBlock = reqRespRewriter(c)(block)

    reify {
      { (request: HttpServletRequest, response: HttpServletResponse) =>
        c.Expr[Any](newBlock).splice
      }
    }
  }

  def routeBuilder(c: DSLContext)(method: c.Expr[HttpMethod],
                                transformers: Seq[c.Expr[RouteTransformer]],
                                block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._

    val texpr = c.Expr[Seq[RouteTransformer]](q"Seq(..$transformers)")

//    println("Before: \n" + block.tree)

    val action = actionBuilder(c)(block)

    val result = reify {
      c.prefix.splice.addRoute(method.splice, texpr.splice, action.splice)
    }

//    println("-------------------------------------------------\n" +
//      result.tree +
//      "\n-------------------------------------------------")

    result
  }

  def beforeImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val routeExpr = actionBuilder(c)(block)
    val texpr = c.Expr[Seq[RouteTransformer]](q"Seq(..$transformers)")

    reify {
      c.prefix.splice.beforeAction(texpr.splice:_*)(routeExpr.splice)
    }
  }

  def afterImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val routeExpr = actionBuilder(c)(block)
    val texpr = c.Expr[Seq[RouteTransformer]](q"Seq(..$transformers)")

    reify {
      c.prefix.splice.afterAction(texpr.splice:_*)(routeExpr.splice)
    }
  }

  def errorImpl(c: DSLContext)(handler: c.Expr[ErrorHandler]): c.Expr[Unit] = {
    import c.universe._

    val rewriteTree = reqRespRewriter(c)(handler)

    reify {
      c.prefix.splice.errorAction( (request, response) => c.Expr[ErrorHandler](rewriteTree).splice )
    }
  }


  def getImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    routeBuilder(c)(c.universe.reify(Get), transformers, block)
  }

  def postImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    routeBuilder(c)(c.universe.reify(Post), transformers, block)
  }

  def putImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    routeBuilder(c)(c.universe.reify(Put), transformers, block)
  }

  def deleteImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    routeBuilder(c)(c.universe.reify(Delete), transformers, block)
  }

  def optionsImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    routeBuilder(c)(c.universe.reify(Options), transformers, block)
  }

  def headImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    routeBuilder(c)(c.universe.reify(Head), transformers, block)
  }

  def patchImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    routeBuilder(c)(c.universe.reify(Patch), transformers, block)
  }

  def notFoundImpl(c: DSLContext)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val action = actionBuilder(c)(block)

    reify {
      c.prefix.splice.notFoundAction {
        action.splice
        //c.Expr[Action](Ident(actionName)).splice
      }
    }
  }

  def trapRangeImpl(c: DSLContext)(codes: c.Expr[Range])(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val action = actionBuilder(c)(block)

    reify {
      c.prefix.splice.trapAction(codes.splice){
        action.splice
        //c.Expr[Action](Ident(actionName)).splice
      }
    }
  }

  def trapImpl(c: DSLContext)(code: c.Expr[Int])(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val rangeExpr = reify {
      val c = code.splice
      Range(c, c+1)
    }

    trapRangeImpl(c)(rangeExpr)(block)
  }

  def booleanBlock2RouteMatcherImpl(c: DSLContext)(block: c.Expr[Boolean]): c.Expr[RouteMatcher] =  {

    import c.universe._

    val newBlock = reqRespRewriter(c)(block)

    reify {
      new BooleanBlockRouteMatcher({ (request: HttpServletRequest, response: HttpServletResponse) =>
        c.Expr[Boolean](newBlock).splice
      })
    }
  }
}

