import org.fusesource.scalate.RenderContext
import org.fusesource.scalate.wikitext.Pygmentize

package

object Website {
  def pygmentizeResource(uri: String, lang: String) = {
    val context = RenderContext()
    val body = context.engine.resourceLoader.load(uri)
    context << Pygmentize.pygmentize(body, "scala")
  }
}