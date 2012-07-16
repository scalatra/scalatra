package org.scalatra
package scalate

import i18n.I18nSupport

import org.fusesource.scalate.{TemplateEngine, Binding, RenderContext}
import i18n.Messages
import java.io.PrintWriter

trait ScalateI18nSupport extends ScalateSupport with I18nSupport {
  this: ScalatraApp =>

//  /*
//   * Binding done here seems to work all the time.
//   *
//   * If it were placed in createRenderContext, it wouldn't work for "view" templates
//   * on first access. However, on subsequent accesses, it worked fine.
//   */
//  before() {
//    templateEngine.bindings ::= Binding("messages", classOf[Messages].getName, true, isImplicit = true)
//  }


  /**
   * Creates the templateEngine from the config.  There is little reason to
   * override this unless you have created a ScalatraKernel extension outside
   * an HttpServlet or Filter.
   */
  override protected def createTemplateEngine(config: AppContext): TemplateEngine = {
    val eng = super.createTemplateEngine(config)
    eng.bindings ::= Binding.of[Messages]("messages", importMembers = true, isImplicit = true)
    eng
  }

  /**
   * Added "messages" into the template context so it can be accessed like:
   * #{messages("hello")}
   */
  override protected def createRenderContext(req: HttpRequest = request, resp: HttpResponse = response, out: PrintWriter = response.writer): RenderContext = {
    val context = new ScalatraRenderContext(this, req, resp)
    context.attributes.update("messages", messages)
    context
  }
}
