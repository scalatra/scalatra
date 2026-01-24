package org.scalatra.forms

import org.scalatra.i18n.*
import org.scalatra.servlet.ServletBase

trait FormSupport { self: ServletBase & I18nSupport =>

  protected def validate[V, T](form: ValueType[V])(hasErrors: Seq[(String, String)] => T, success: V => T): T = {
    val params = multiParams
    request.setAttribute(RequestAttributeParamsKey, params)

    val errors = form.validate("", params, messages)
    if (errors.isEmpty) {
      success(form.convert("", params, messages))
    } else {
      request.setAttribute(RequestAttributeErrorsKey, errors)
      hasErrors(errors)
    }
  }

}
