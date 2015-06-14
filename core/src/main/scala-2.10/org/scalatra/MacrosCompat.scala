package org.scalatra

object MacrosCompat {

  type Context = scala.reflect.macros.Context

  def freshName[C <: Context](c: C)(name: String): String = c.fresh(name)

  def typeName[C <: Context](c: C)(name: String): c.universe.TypeName  = c.universe.newTypeName(name)

  def termName[C <: Context](c: C)(name: String): c.universe.TermName = c.universe.newTermName(name)

  def typecheck[C <: Context](c: C)(tree: c.universe.Tree): c.universe.Tree = c.typeCheck(tree)

  def untypecheck[C <: Context](c: C)(tree: c.universe.Tree): c.universe.Tree = c.resetLocalAttrs(tree)

}
