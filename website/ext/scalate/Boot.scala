package scalate

import org.fusesource.scalate._

class Boot(engine: TemplateEngine) {
  def run: Unit = {
    for (ssp <- engine.filter("ssp"); md <- engine.filter("markdown")) {
      engine.pipelines += "ssp.md"-> List(ssp, md)
    }
  }
}