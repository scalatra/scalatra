package org.scalatra

// defines stubs for stuff that's missing in Scala 2.10
object Compat210 {
  object blackbox { // scala.reflect.macros.blackbox package
    type Context = scala.reflect.macros.Context
  }
  object internal // Context.internal object
  object decorators // Context.decorators object
  object contexts // scala.reflect.macros.contexts package
}
import Compat210._

// unifies scala.reflect.macros.runtime.Context (Scala 2.10)
// and scala.reflect.macros.contexts.Context (Scala 2.11)
object Power {
  import scala.reflect.macros._
  object DummyScope {
    import runtime._
    import contexts._
    type Result = Context
  }
  type PowerContext = DummyScope.Result
}
import Power._

// a cake slice that can be mixed into improvised macro bundles
// to transparently bring new Scala 2.11 features to Scala 2.10
trait Internal210 { self =>
  import scala.reflect.macros._
  import blackbox.Context

  val c: Context
  import c.universe._

  // enrichments that backport parts of Scala 2.11's c.internal to Scala 2.10
  // these are only going to be picked up if we compile against Scala 2.10
  implicit class RichContext(val c: self.c.type) {
    object internal {
      def enclosingOwner: Symbol = {
        val powerContext = c.asInstanceOf[PowerContext]
        powerContext.callsiteTyper.context.owner.asInstanceOf[Symbol]
      }
      def changeOwner(tree: Tree, oldOwner: Symbol, newOwner: Symbol): Tree = {
        val powerContext = c.asInstanceOf[PowerContext]
        val global = powerContext.universe
        class ChangeOwnerAndModuleClassTraverser(oldOwner: global.Symbol, newOwner: global.Symbol) extends global.ChangeOwnerTraverser(oldOwner, newOwner) {
          override def traverse(tree: global.Tree) {
            tree match {
              case _: global.DefTree => change(tree.symbol.moduleClass)
              case _ =>
            }
            super.traverse(tree)
          }
        }
        val traverser = new ChangeOwnerAndModuleClassTraverser(oldOwner.asInstanceOf[global.Symbol], newOwner.asInstanceOf[global.Symbol])
        traverser.traverse(tree.asInstanceOf[global.Tree])
        tree
      }
      def valDef(sym: Symbol, rhs: Tree): ValDef = {
        val powerContext = c.asInstanceOf[PowerContext]
        val global = powerContext.universe
        global.ValDef(sym.asInstanceOf[global.Symbol], rhs.asInstanceOf[global.Tree]).asInstanceOf[ValDef]
      }
      trait TypingTransformApi {
        def default(tree: Tree): Tree
        def typecheck(tree: Tree): Tree
      }
      def typingTransform(tree: Tree)(transformer: (Tree, TypingTransformApi) => Tree): Tree = {
        val powerContext = c.asInstanceOf[PowerContext]
        val global = powerContext.universe
        class MiniCake[G <: scala.tools.nsc.Global](val global: G) extends scala.tools.nsc.transform.TypingTransformers {
          val callsiteTyper = powerContext.callsiteTyper.asInstanceOf[global.analyzer.Typer]
          class HofTypingTransformer(hof: (Tree, TypingTransformApi) => Tree) extends TypingTransformer(callsiteTyper.context.unit) { self =>
            currentOwner = callsiteTyper.context.owner
            curTree = global.EmptyTree
            localTyper = global.analyzer.newTyper(callsiteTyper.context.make(unit = callsiteTyper.context.unit))
            val api = new TypingTransformApi {
              def default(tree: Tree): Tree = superTransform(tree.asInstanceOf[global.Tree]).asInstanceOf[Tree]
              def typecheck(tree: Tree): Tree = localTyper.typed(tree.asInstanceOf[global.Tree]).asInstanceOf[Tree]
            }
            def superTransform(tree: global.Tree) = super.transform(tree)
            override def transform(tree: global.Tree): global.Tree = hof(tree.asInstanceOf[Tree], api).asInstanceOf[global.Tree]
          }
        }
        val miniCake = new MiniCake[global.type](global)
        new miniCake.HofTypingTransformer(transformer).transform(tree.asInstanceOf[global.Tree]).asInstanceOf[Tree]
      }
    }
  }

  // we can't use Symbol.setInfo in Scala 2.10 (doesn't exist yet) or Symbol.setTypeSignature in Scala 2.11 (has been removed)
  // therefore we need to settle on some sort of a middle ground
  implicit class RichSymbol(val sym: self.c.universe.Symbol) {
    def setInfoCompat(info: Type): Symbol = {
      import compat._
      sym.setTypeSignature(info)
    }
  }
}