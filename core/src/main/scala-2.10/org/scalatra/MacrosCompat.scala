package org.scalatra

trait MacrosCompat extends Internal210 {

  type Context = scala.reflect.macros.Context

  def freshName(name: String): String = c.fresh(name)

  def typeName(name: String): c.universe.TypeName  = c.universe.newTypeName(name)

  def termName(name: String): c.universe.TermName = c.universe.newTermName(name)

  def typecheck(tree: c.universe.Tree): c.universe.Tree = c.typeCheck(tree)

  def untypecheck(tree: c.universe.Tree): c.universe.Tree = c.resetLocalAttrs(tree)

}
