package org.scalatra.forms

import org.scalatra.i18n.Messages

trait ValueType[T] {

  def convert(name: String, params: Map[String, Seq[String]], messages: Messages): T

  def validate(name: String, params: Map[String, Seq[String]], messages: Messages): Seq[(String, String)]

  def verifying(validator: (T, Map[String, Seq[String]]) => Seq[(String, String)]): ValueType[T] =
    new VerifyingValueType(this, validator)

  def verifying(validator: (T) => Seq[(String, String)]): ValueType[T] =
    new VerifyingValueType(this, (value: T, params: Map[String, Seq[String]]) => validator(value))

}

/**
 * The base class for the single field ValueTypes.
 */
abstract class SingleValueType[T](constraints: Constraint*) extends ValueType[T] {

  def convert(name: String, params: Map[String, Seq[String]], messages: Messages): T =
    convert(getSingleParam(params, name).orNull, messages)

  def convert(value: String, messages: Messages): T

  def validate(name: String, params: Map[String, Seq[String]], messages: Messages): Seq[(String, String)] =
    validate(name, getSingleParam(params, name).orNull, params, messages)

  def validate(name: String, value: String, params: Map[String, Seq[String]], messages: Messages): Seq[(String, String)] =
    validaterec(name, value, params, Seq(constraints: _*), messages)

  @scala.annotation.tailrec
  private def validaterec(name: String, value: String, params: Map[String, Seq[String]],
    constraints: Seq[Constraint], messages: Messages): Seq[(String, String)] = {
    constraints match {
      case (x :: rest) => x.validate(name, value, params, messages) match {
        case Some(message) => Seq(name -> message)
        case None => validaterec(name, value, params, rest, messages)
      }
      case _ => Nil
    }
  }

}

/**
 * ValueType wrapper to verify the converted value.
 * An instance of this class is returned from only [[ValueType#verifying]].
 *
 * @param valueType the wrapped ValueType
 * @param validator the function which verifies the converted value
 */
private[forms] class VerifyingValueType[T](
  valueType: ValueType[T],
  validator: (T, Map[String, Seq[String]]) => Seq[(String, String)]) extends ValueType[T] {

  def convert(name: String, params: Map[String, Seq[String]], messages: Messages): T = valueType.convert(name, params, messages)

  def validate(name: String, params: Map[String, Seq[String]], messages: Messages): Seq[(String, String)] = {
    val result = valueType.validate(name, params, messages)
    if (result.isEmpty) {
      validator(convert(name, params, messages), params)
    } else {
      result
    }
  }
}

/**
 * The base class for the object field ValueTypes.
 */
abstract class MappingValueType[T] extends ValueType[T] {

  def fields: Seq[(String, ValueType[?])]

  def validate(name: String, params: Map[String, Seq[String]], messages: Messages): Seq[(String, String)] = {
    fields.flatMap {
      case (fieldName, valueType) =>
        valueType.validate((if (name.isEmpty) fieldName else name + "." + fieldName), params, messages)
    }
  }

}
