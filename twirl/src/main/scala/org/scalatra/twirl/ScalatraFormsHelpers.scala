package org.scalatra.twirl

import jakarta.servlet.http.HttpServletRequest

import org.scalatra.MultiParams
import play.twirl.api.Html

trait ScalatraFormsHelpers {

  private val RequestAttributeParamsKey = "org.scalatra.forms.params"
  private val RequestAttributeErrorsKey = "org.scalatra.forms.errors"

  /**
   * Render a text field.
   */
  def text(name: String, attributes: (String, String)*)(implicit request: HttpServletRequest): Html = {
    Html(s"""<input type="text" name="${escape(name)}" value="${escape(param(name))}" ${attrs(attributes: _*)}>""")
  }

  /**
   * Render a password field.
   */
  def password(name: String, attributes: (String, String)*)(implicit request: HttpServletRequest): Html = {
    Html(s"""<input type="password" name="${escape(name)}" ${attrs(attributes: _*)}>""")
  }

  /**
   * Render a textarea.
   */
  def textarea(name: String, attributes: (String, String)*)(implicit request: HttpServletRequest): Html = {
    Html(s"""<textarea name="${escape(name)}" ${attrs(attributes: _*)}>${escape(param(name))}</textarea>""")
  }

  /**
   * Render a checkbox.
   */
  def checkbox(name: String, value: String, attributes: (String, String)*)(implicit request: HttpServletRequest): Html = {
    val checked = if (params(name).contains(value)) "checked" else ""
    Html(s"""<input type="checkbox" name="${escape(name)}" value="${escape(value)}" $checked ${attrs(attributes: _*)}>""")
  }

  /**
   * Render a radio button.
   */
  def radio(name: String, value: String, attributes: (String, String)*)(implicit request: HttpServletRequest): Html = {
    val checked = if (param(name) == value) "checked" else ""
    Html(s"""<input type="radio" name="${escape(name)}" value="${escape(value)}" $checked ${attrs(attributes: _*)}>""")
  }

  /**
   * Render a select box.
   */
  def select(name: String, values: Seq[(String, String)], multiple: Boolean, attributes: (String, String)*)(implicit request: HttpServletRequest): Html = {
    val sb = new StringBuilder()
    sb.append(s"""<select name="${escape(name)}" ${if (multiple) "multiple" else ""}>""")
    values.foreach {
      case (value, label) =>
        val selected = if (params(name).contains(value)) "selected" else ""
        sb.append(s"""<option value="${escape(value)}" $selected>${escape(label)}</option>""")
    }
    sb.append("</select>")
    Html(sb.toString)
  }

  /**
   * Retrieve an error message of the specified field.
   */
  def error(name: String)(implicit request: HttpServletRequest): Option[String] = {
    Option(request.getAttribute(RequestAttributeErrorsKey)).flatMap { errors =>
      errors.asInstanceOf[Seq[(String, String)]].find(_._1 == name).map(_._2)
    }
  }

  /**
   * Retrieve all error messages of the specified field.
   */
  def errors(name: String)(implicit request: HttpServletRequest): Seq[String] = {
    Option(request.getAttribute(RequestAttributeErrorsKey)).map { errors =>
      errors.asInstanceOf[Seq[(String, String)]].collect { case error if error._1 == name => error._2 }
    }.getOrElse(Nil)
  }

  private def escape(value: String): String = {
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
  }

  private def params(name: String)(implicit request: HttpServletRequest): Seq[String] = {
    Option(request.getAttribute(RequestAttributeParamsKey)).flatMap { params =>
      params.asInstanceOf[MultiParams].get(name)
    }.getOrElse(Nil)
  }

  private def param(name: String)(implicit request: HttpServletRequest): String = {
    params(name).headOption.getOrElse("")
  }

  private def attrs(attrs: (String, String)*): String = {
    attrs.map { case (name, value) => s"""${escape(name)}="${escape(value)}"""" }.mkString(" ")
  }

}

/**
 * Use helper methods defined in `ScalatraFormsHelpers` in Twirl templates as follows:
 *
 * {{{
 * @()(implicit request: jakarta.servlet.http.HttpServletRequest)
 * @import org.scalatra.twirl.forms._
 *
 * <form action="/login" method="POST">
 *   UserId: @text("userName") @errors("userName")<br>
 *   Password: @password("password") @errors("password")<br>
 *   <input type="submit" value="Login">
 * </form>
 * }}}
 */
object forms extends ScalatraFormsHelpers
