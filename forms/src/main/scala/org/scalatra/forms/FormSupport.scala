package org.scalatra.forms

import org.scalatra.i18n._
import org.scalatra.servlet.ServletBase

trait FormSupport { self: ServletBase with I18nSupport =>

  protected def validate[T](form: ValueType[T])(hasErrors: Seq[(String, String)] => Any, success: T => Any): Any = {
    val paramMap = params.toSeq.toMap
    val errors = form.validate("", paramMap, messages)
    if (errors.isEmpty) {
      success(form.convert("", paramMap, messages))
    } else {
      hasErrors(errors)
    }
  }

}
