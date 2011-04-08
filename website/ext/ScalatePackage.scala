import org.fusesource.scalate.support.TemplatePackage
import org.fusesource.scalate.{TemplateSource, Binding}

class ScalatePackage extends TemplatePackage {
  def header(source: TemplateSource, bindings: List[Binding]) =
    """
    import _root_.Website._
    """
}