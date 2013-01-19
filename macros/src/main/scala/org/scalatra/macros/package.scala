package org.scalatra

import scala.reflect.macros.Context
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

package object macros {
  def requestImpl(c: Context): c.Expr[HttpServletRequest] = {
    c.universe.reify { null: HttpServletRequest }
  }

  def responseImpl(c: Context): c.Expr[HttpServletResponse] = {
    c.universe.reify { null: HttpServletResponse }
  }

  import HttpMethod._

  def getImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] =
    addRoute(c, Get)(transformers: _*)(block)

  def postImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] =
    addRoute(c, Post)(transformers: _*)(block)

  def putImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] =
    addRoute(c, Put)(transformers: _*)(block)

  def deleteImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] =
    addRoute(c, Delete)(transformers: _*)(block)

  def optionsImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] =
    addRoute(c, Options)(transformers: _*)(block)

  def patchImpl(c: Context)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] =
    addRoute(c, Patch)(transformers: _*)(block)

  def addRoute[C <: Context](c: Context, method: HttpMethod)(transformers: c.Expr[RouteTransformer]*)(block: c.Expr[Any]): c.Expr[Route] = {
    c.universe.reify { null: Route }
  }
}
