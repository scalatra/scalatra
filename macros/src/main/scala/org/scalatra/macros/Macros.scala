package org.scalatra
package macros

import scala.languageFeature.experimental.macros._
import scala.reflect.macros.Context

object Macros {
  def before(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    reify { ??? }
  }

  def after(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    reify { ??? }
  }

  def get(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def post(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def put(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def delete(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def options(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def patch(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def notFound(c: Context)(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    reify { ??? }
  }

  def trapRange(c: Context)(codes: c.Expr[Range])(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    reify { ??? }
  }

  def trapCode(c: Context)(code: c.Expr[Int])(block: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    reify { ??? }
  }

  def asyncGet(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def asyncPost(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def asyncPut(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def asyncDelete(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def asyncOptions(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }

  def asyncPatch(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    import c.universe._
    reify { ??? }
  }
}
