package org.scalatra
package scalate

import i18n.I18nSupport

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.fusesource.scalate.Binding
import org.fusesource.scalate.RenderContext
import javax.servlet.ServletConfig
import org.fusesource.scalate.servlet.ServletTemplateEngine
import javax.servlet.FilterConfig
import org.fusesource.scalate.TemplateEngine
import i18n.Messages
import java.io.PrintWriter

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
  override protected def createRenderContext(req: HttpServletRequest = request, resp: HttpServletResponse = response, out: PrintWriter = response.getWriter): RenderContext = {
    val context = super.createRenderContext(req, resp, out)
    context.attributes("messages") = messages(request)
    context
  }
}
