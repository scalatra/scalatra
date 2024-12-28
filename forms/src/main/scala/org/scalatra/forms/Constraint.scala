package org.scalatra.forms

import org.scalatra.i18n.Messages

trait Constraint {

  def validate(
      name: String,
      value: String,
      params: Map[String, Seq[String]],
      messages: Messages
  ): Option[String] =
    validate(name, value, messages)

  def validate(
      name: String,
      value: String,
      messages: Messages
  ): Option[String] = None

}
