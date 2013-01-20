package org.scalatra.plugins

import scala.tools.nsc
import nsc.Global
import nsc.Phase
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent
import nsc.transform.{ Transform, TypingTransformers }
import nsc.symtab.Flags

class RrpsPlugin(val global: Global) extends Plugin {
  import global._

  val name = "rrps"
  val description = ""
  val components = List[PluginComponent](ExampleComponent)

  // a sample component which is a transformer
  // which replaces all literal string constants
  // in the compiled sources
  private object ExampleComponent extends PluginComponent with Transform {
  
    import global._
    import global.definitions._

    val global = RrpsPlugin.this.global

    val symRequestResponseScope = definitions.getClass("org.scalatra.RequestResponseScope")

    // TODO: change that according to your requirements
    override val runsAfter = List("typer")

    /** The phase name of the compiler plugin
     *  @todo Adapt to specific plugin.
     */
    val phaseName = "RRPS"

    def newTransformer(unit: CompilationUnit) = new RrpsTransformer(unit)

    class RrpsTransformer(unit: CompilationUnit) extends Transformer {
      // TODO: fill in your logic here
      override def transform(tree: Tree): Tree = tree match {
        case Select(qualifier, name) if Set("request", "response").contains(name.decode) =>
          val isSub = qualifier.symbol.typeOfThis.baseClasses.exists(_ == symRequestResponseScope)
          if (isSub) unit.warning(tree.pos, "unsafe "+name+" access")
          tree
        case _ => super.transform(tree)
      }
    }
  }
}
