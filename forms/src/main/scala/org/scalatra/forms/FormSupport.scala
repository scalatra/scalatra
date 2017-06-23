package org.scalatra.forms

import org.scalatra.i18n._
import org.scalatra.servlet.ServletBase

trait FormSupport { self: ServletBase with I18nSupport =>

  protected def validate[T](form: ValueType[T])(hasErrors: Seq[(String, String)] => Any, success: T => Any): Any = {
    val errors = form.validate("", multiParams, messages)
    if (errors.isEmpty) {
      success(form.convert("", multiParams, messages))
    } else {
      hasErrors(errors)
    }
  }

}
