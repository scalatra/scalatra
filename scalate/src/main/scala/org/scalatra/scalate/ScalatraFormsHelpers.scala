package org.scalatra.scalate

import org.fusesource.scalate.servlet.ServletRenderContext
import org.scalatra.forms

/**
 * Supplies helper methods to render forms in Scalate templates.
 */
trait ScalatraFormsHelpers { self: ServletRenderContext =>

  def text(name: String, attributes: (String, String)*): Unit = unescape(forms.views.text(name, attributes: _*)(request))
  def password(name: String, attributes: (String, String)*): Unit = unescape(forms.views.password(name, attributes: _*)(request))
  def textarea(name: String, attributes: (String, String)*): Unit = unescape(forms.views.textarea(name, attributes: _*)(request))
  def checkbox(name: String, value: String, attributes: (String, String)*): Unit = unescape(forms.views.checkbox(name, value, attributes: _*)(request))
  def radio(name: String, value: String, attributes: (String, String)*): Unit = unescape(forms.views.radio(name, value, attributes: _*)(request))
  def select(name: String, values: Seq[(String, String)], multiple: Boolean, attributes: (String, String)*): Unit = unescape(forms.views.select(name, values, multiple, attributes: _*)(request))
  def error(name: String): Option[String] = forms.views.error(name)(request)
  def errors(name: String): Seq[String] = forms.views.errors(name)(request)

}
