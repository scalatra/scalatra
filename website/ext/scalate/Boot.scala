package scalate

import java.util.regex.Matcher
import org.fusesource.scalamd.{MacroDefinition, Markdown}
import org.fusesource.scalate._
import org.fusesource.scalate.wikitext.Pygmentize

class Boot(engine: TemplateEngine) {
  def run: Unit = {
    def pygmentize(m: Matcher): String = Pygmentize.pygmentize(m.group(2), m.group(1))

    Markdown.macros :::= List(MacroDefinition("""\{pygmentize::(.*?)\}(.*?)\{pygmentize\}""", "s", pygmentize, true))

    for (ssp <- engine.filter("ssp"); md <- engine.filter("markdown")) {
      engine.pipelines += "ssp.md"-> List(ssp, md)
    }
  }
}