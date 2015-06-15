package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.fusesource.scalate.{ Binding, RenderContext }
import org.scalatra.i18n.{ I18nSupport, Messages }

trait ScalateI18nSupport extends ScalateSupport with I18nSupport {

  /*
   * Binding done here seems to work all the time. 
   * 
   * If it were placed in createRenderContext, it wouldn't work for "view" templates
   * on first access. However, on subsequent accesses, it worked fine. 
   */
  before() {
    templateEngine.bindings ::= Binding("messages", classOf[Messages].getName, true, isImplicit = true)
  }

  /**
   * Added "messages" into the template context so it can be accessed like:
   * #{messages("hello")}
   */
  override protected def createRenderContext(out: PrintWriter)(implicit request: HttpServletRequest, response: HttpServletResponse): RenderContext = {
    val context = super.createRenderContext(out)
    context.attributes("messages") = messages(request)
    context
  }
}
