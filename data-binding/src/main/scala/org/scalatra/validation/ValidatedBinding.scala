//package org.scalatra
//package validation
//
//import org.scalatra.databinding.Binding
//
///**
// * A field [[org.scalatra.command.Binding]] which value has been validated.
// */
//trait ValidatedBinding[T] extends Binding[T]  {
//
//  /**
//   * Result of command. Either one of @Rejected or @Accepted
//   */
//  def validation: FieldValidation[T]
//
//  /**
//   * Check whether the the field value conforms to the user requirements.
//   */
//  def valid = validation.isSuccess
//
//  /**
//   * The rejected message, if any.
//   */
//  def rejected = validation.fail.toOption
//}
//
//class ValidatedBindingDecorator[T](val validator: Validator[T], binding: Binding[T]) extends ValidatedBinding[T] {
//
//  lazy val validation = validator(converted)
//
//  def name = binding.name
//
//  def original = binding.original
//
//  def converted = binding.converted
//
//  def apply(value: String) = binding.apply(value)
//
//  override def hashCode() = binding.hashCode()
//
//  override def equals(obj: Any) = binding.equals(obj)
//}
//
