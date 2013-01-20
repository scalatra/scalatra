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
    
    // TODO: change that according to your requirements
    override val runsAfter = List("parser")

    /** The phase name of the compiler plugin
     *  @todo Adapt to specific plugin.
     */
    val phaseName = "RRPS"

    def newTransformer(unit: CompilationUnit) = new RrpsTransformer(unit)

    class RrpsTransformer(unit: CompilationUnit) extends Transformer {
      // TODO: fill in your logic here
      override def transform(tree: Tree): Tree = tree match {
        case Literal(Constant(str: String)) => Literal(Constant("ICanHazYourStrngLiterls"))

        // don't forget this case, so that tree is actually traversed
        case _ => super.transform(tree)
      }
    }
  }
}
