package org.scalatra.atmosphere.macros

import scala.reflect.macros.Context


import org.scalatra.atmosphere.{AtmoAction, AtmosphereClient, AtmosphereSupport}
import org.scalatra.{Route, RouteTransformer}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
 * @author Bryce Anderson
 *         Created on 3/11/14
 */
object AtmosphereMacros {

  import org.scalatra.macros.DSLMacros.reqRespRewriter

  type DSLContext = Context { type PrefixType = AtmosphereSupport }

  //(transformers: RouteTransformer*)(block: AtmosphereClient)

  def atmosphereImpl(c: DSLContext)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[AtmosphereClient]): c.Expr[Route] = {
    import c.universe._

    val newTree = reqRespRewriter(c)(block)

    val texpr = c.Expr[Seq[RouteTransformer]](q"Seq(..$transformers)")

    val actionExpr = reify {
      (request: HttpServletRequest, response: HttpServletResponse) =>
        c.Expr[AtmosphereClient](newTree).splice
    }

    reify {
      c.prefix.splice.atmosphereAction(texpr.splice :_*)(actionExpr.splice)
    }


  }


}
