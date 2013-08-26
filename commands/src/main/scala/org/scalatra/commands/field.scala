package org.scalatra
package commands

import validation._
import util.conversion._
import scalaz._
import syntax.validation._
import syntax.std.option._
import mojolly.inflector.InflectorImports._
import org.scalatra.util.RicherString._
import org.json4s.JsonFormat

object DefVal {
  def apply[T:Manifest](prov: => T) = new DefVal(prov)
}
class DefVal[T:Manifest](valueProvider: => T) {
  lazy val value = valueProvider
}
object ValueSource extends Enumeration {
  val Header = Value("header")
  val Body = Value("body")
  val Query = Value("query")
  val Path = Value("path")
}

object FieldDescriptor {
  def apply[T](name: String)(implicit mf: Manifest[T], defV: DefaultValue[T]): FieldDescriptor[T] = 
    new BasicFieldDescriptor[T](name, transformations = identity, defVal = DefVal(defV.default))
}
trait FieldDescriptor[T] {

  def name: String
  def value: FieldValidation[T]
  def validator: Option[Validator[T]]
  def notes: String 
  def notes(note: String): FieldDescriptor[T]
  def description: String 
  def description(desc: String): FieldDescriptor[T]
  def valueManifest: Manifest[T]
  def valueSource: ValueSource.Value
  def sourcedFrom(valueSource: ValueSource.Value): FieldDescriptor[T]
  def allowableValues: List[T]
  def allowableValues(vals: T*): FieldDescriptor[T]
  def displayName: Option[String]
  def displayName(name: String): FieldDescriptor[T]
  
  private[commands] def defVal: DefVal[T]
  def defaultValue: T = defVal.value
  def withDefaultValue(default: => T): FieldDescriptor[T]

  def isValid = value.isSuccess
  def isInvalid = value.isFailure

  private[commands] def isRequired: Boolean
  def required: FieldDescriptor[T]
  def optional: FieldDescriptor[T]

  override def toString() = "FieldDescriptor(name: %s)".format(name)

  def validateWith(validators: BindingValidator[T]*): FieldDescriptor[T]

  def apply[S](original: Either[String, Option[S]])(implicit ms: Manifest[S], df: DefaultValue[S], convert: TypeConverter[S, T]): DataboundFieldDescriptor[S, T]

  override def hashCode() = 41 + 41 * name.hashCode()

  def transform(endo: T => T): FieldDescriptor[T]

  private[commands] def transformations: T => T

  override def equals(obj: Any) = obj match {
    case b : FieldDescriptor[_] => b.name == this.name
    case _ => false
  }

}

class BasicFieldDescriptor[T](
    val name: String, 
    val validator: Option[Validator[T]] = None, 
    private[commands] val transformations: T => T = identity _,
    private[commands] var isRequired: Boolean = false,
    val description: String = "",
    val notes: String = "",
    private[commands] val defVal: DefVal[T],
    val valueSource: ValueSource.Value = ValueSource.Body,
    val allowableValues: List[T] = Nil,
    val displayName: Option[String] = None)(implicit val valueManifest: Manifest[T]) extends FieldDescriptor[T] {

  val value: FieldValidation[T] = defaultValue.success

  def validateWith(bindingValidators: BindingValidator[T]*): FieldDescriptor[T] = {
    val nwValidators: Option[Validator[T]] =
      if(bindingValidators.nonEmpty) Some(bindingValidators.map(_ apply name).reduce(_ andThen _)) else None

    copy(validator = validator.flatMap(v => nwValidators.map(v andThen _)) orElse nwValidators)
  }

  def copy(
      name: String = name, 
      validator: Option[Validator[T]] = validator, 
      transformations: T => T = transformations, 
      isRequired: Boolean = isRequired, 
      description: String = description, 
      notes: String = notes,
      defVal: DefVal[T] = defVal,
      valueSource: ValueSource.Value = valueSource,
      allowableValues: List[T] = allowableValues,
      displayName: Option[String] = displayName): FieldDescriptor[T] = {
    new BasicFieldDescriptor(name, validator, transformations, isRequired, description, notes, defVal, valueSource, allowableValues, displayName)(valueManifest)
  }

  def apply[S](original: Either[String, Option[S]])(implicit ms: Manifest[S], df: DefaultValue[S], convert: TypeConverter[S, T]): DataboundFieldDescriptor[S, T] = {
    val defValS = df.default
    val conv = original.fold(e => ValidationError(e).fail, o => (convert(o | defValS) | defaultValue).success)
    val o = original.fold(_ => None, identity)
    BoundFieldDescriptor(o, conv, this)
  }

  def transform(endo: T => T): FieldDescriptor[T] = copy(transformations = transformations andThen endo)

  def required = copy(isRequired = true)

  def optional = copy(isRequired = false)
  
  def description(desc: String) = copy(description = desc)
  
  def notes(note: String) = copy(notes = note)
  
  def withDefaultValue(default: => T): FieldDescriptor[T] = copy(defVal = DefVal(default), isRequired = false)
  
  def sourcedFrom(valueSource: ValueSource.Value): FieldDescriptor[T] = copy(valueSource = valueSource)

  def allowableValues(vals: T*): FieldDescriptor[T] =
    copy(allowableValues = vals.toList).validateWith(BindingValidators.oneOf("%%s must be one of %s.", vals))

  def displayName(name: String): FieldDescriptor[T] = copy(displayName = name.blankOption)
}


trait DataboundFieldDescriptor[S, T] extends FieldDescriptor[T] {
  def field: FieldDescriptor[T]
  def original: Option[S]
  def transform(endo: T => T): DataboundFieldDescriptor[S, T]
  def apply[V](original: Either[String, Option[V]])(implicit mv: Manifest[V], df: DefaultValue[V], convert: TypeConverter[V, T]): DataboundFieldDescriptor[V, T] =
    this.asInstanceOf[DataboundFieldDescriptor[V, T]]

  override def toString() = "FieldDescriptor(name: %s, original: %s, value: %s)".format(name, original, value)
  def validate: ValidatedFieldDescriptor[S, T]
  def validateWith(bindingValidators: BindingValidator[T]*): DataboundFieldDescriptor[S, T]
  def required: DataboundFieldDescriptor[S, T]
  def optional: DataboundFieldDescriptor[S, T]
  def isRequired = field.isRequired
  def description = field.description
  def description(desc: String): DataboundFieldDescriptor[S, T] 
  def notes = field.notes
  def notes(note: String): DataboundFieldDescriptor[S, T]
  def valueManifest = field.valueManifest
  private[commands] def defVal: DefVal[T] = field.defVal
  def withDefaultValue(default: => T): DataboundFieldDescriptor[S, T] 
  def valueSource: ValueSource.Value = field.valueSource
  def sourcedFrom(valueSource: ValueSource.Value): DataboundFieldDescriptor[S, T]
  def allowableValues = field.allowableValues
  def allowableValues(vals: T*): DataboundFieldDescriptor[S, T]
  def displayName: Option[String] = field.displayName
  def displayName(name: String): DataboundFieldDescriptor[S, T]
  
}

trait ValidatedFieldDescriptor[S, T] extends DataboundFieldDescriptor[S, T] {
  def validate: ValidatedFieldDescriptor[S, T] = this
}

object BoundFieldDescriptor {
  def apply[S:DefaultValue, T](original: Option[S], value: FieldValidation[T], binding: FieldDescriptor[T]): DataboundFieldDescriptor[S, T] =
    new BoundFieldDescriptor(original, value, binding, binding.validator)
}


class BoundFieldDescriptor[S:DefaultValue, T](
    val original: Option[S],
    val value: FieldValidation[T], 
    val field: FieldDescriptor[T], 
    val validator: Option[Validator[T]]) extends DataboundFieldDescriptor[S, T] {
  def name: String = field.name
  

  override def hashCode(): Int = field.hashCode()
  override def equals(other: Any) = other match {
    case o: BasicFieldDescriptor[T] => field.equals(o)
    case o: BoundFieldDescriptor[T, S] => field.equals(o.field)
    case _ => false
  }
  override def toString() = "BoundFieldDescriptor(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): DataboundFieldDescriptor[S, T] = {
    val nwFld = field.validateWith(bindingValidators:_*)
    copy(field = nwFld, validator = nwFld.validator)
  }

  def copy(original: Option[S] = original, value: FieldValidation[T] = value, field: FieldDescriptor[T] = field, validator: Option[Validator[T]] = validator): DataboundFieldDescriptor[S, T] =
    new BoundFieldDescriptor(original, value, field, validator)

  def transform(endo: T => T): DataboundFieldDescriptor[S, T] = copy(value = value map endo)

  def required = copy(field = field.required)

  def optional = copy(field = field.optional)
  
  def description(desc: String) = copy(field = field.description(desc))
  
  def notes(note: String) = copy(field = field.notes(note))

  def validate: ValidatedFieldDescriptor[S, T] = {
    val defaultValidator: Validator[T] = validator getOrElse identity
    if (!isRequired && original.isEmpty) {
      new ValidatedBoundFieldDescriptor(value map transformations, this)
    } else {
      val doValidation: Validator[T] = if (isRequired) {
        (x: FieldValidation[T]) => x flatMap { v =>
          if (original.isDefined) v.success else ValidationError("%s is required." format(name.underscore.humanize), FieldName(name), ValidationFail).fail
        }
      } else identity
      new ValidatedBoundFieldDescriptor((doValidation andThen defaultValidator)(value) map transformations, this)
    }
  }

  private[commands] def transformations: (T) => T = field.transformations
  
  def withDefaultValue(default: => T): DataboundFieldDescriptor[S, T] = copy(field = field.withDefaultValue(default))
  
  def sourcedFrom(valueSource: ValueSource.Value): DataboundFieldDescriptor[S, T] = copy(field = field.sourcedFrom(valueSource))

  def allowableValues(vals: T*): DataboundFieldDescriptor[S, T] =
    copy(field = field.allowableValues(vals:_*))

  def displayName(name: String): DataboundFieldDescriptor[S, T] = copy(field = field.displayName(name))
}

class ValidatedBoundFieldDescriptor[S, T](val value: FieldValidation[T], val field: DataboundFieldDescriptor[S, T]) extends ValidatedFieldDescriptor[S, T] {
  def name: String = field.name

  override def hashCode(): Int = field.hashCode()
  override def equals(other: Any) = other match {
    case o: BasicFieldDescriptor[T] => field.equals(o)
    case o: BoundFieldDescriptor[T, S] => field.equals(o.field)
    case o: ValidatedBoundFieldDescriptor[S, T] => field.equals(o.field)
    case _ => false
  }
  override def toString() = "BoundFieldDescriptor(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): DataboundFieldDescriptor[S, T] = {
    copy(field = field.validateWith(bindingValidators:_*))
  }

  def copy(value: FieldValidation[T] = value, field: DataboundFieldDescriptor[S, T] = field): ValidatedFieldDescriptor[S, T] =
    new ValidatedBoundFieldDescriptor(value, field)

  def transform(endo: T => T): DataboundFieldDescriptor[S, T] = copy(value = value map endo)

  def required = copy(field = field.required)

  def optional = copy(field = field.optional)
   
  def description(desc: String) = copy(field = field.description(desc))
  
  def notes(note: String) = copy(field = field.notes(note))

  def validator: Option[Validator[T]] = field.validator

  def original: Option[S] = field.original

  private[commands] def transformations: (T) => T = field.transformations
  
  def withDefaultValue(default: => T): DataboundFieldDescriptor[S, T] = copy(field = field.withDefaultValue(default))
  
  def sourcedFrom(valueSource: ValueSource.Value): DataboundFieldDescriptor[S, T] = copy(field = field.sourcedFrom(valueSource))
  
  def allowableValues(vals: T*): DataboundFieldDescriptor[S, T] = copy(field = field.allowableValues(vals:_*))
  
  def displayName(name: String): DataboundFieldDescriptor[S, T] = copy(field = field.displayName(name))
}

import scala.util.matching.Regex


trait BindingValidatorImplicits {

  import BindingValidators._
  implicit def validatableStringBinding(b: FieldDescriptor[String]) = new ValidatableStringBinding(b)
  implicit def validatableSeqBinding[T <: Seq[_]](b: FieldDescriptor[T]) = new ValidatableSeq(b)
  implicit def validatableGenericBinding[T](b: FieldDescriptor[T]) = new ValidatableGenericBinding(b)
  implicit def validatableOrderedBinding[T <% Ordered[T]](b: FieldDescriptor[T]) = new ValidatableOrdered(b)

}

object BindingValidators {


  class ValidatableSeq[T <: Seq[_]](b: FieldDescriptor[T]) {
    def notEmpty: FieldDescriptor[T] = notEmpty()
    def notEmpty(messageFormat: String = "%s is required."): FieldDescriptor[T] =
      b.required.validateWith(BindingValidators.nonEmptyCollection(messageFormat))
  }


  class ValidatableOrdered[T <% Ordered[T]](b: FieldDescriptor[T]) {
    def greaterThan(min: T, messageFormat: String = "%%s must be greater than %s"): FieldDescriptor[T] =
      b.validateWith(BindingValidators.greaterThan(min, messageFormat))

    def lessThan(max: T, messageFormat: String = "%%s must be less than %s"): FieldDescriptor[T] =
      b.validateWith(BindingValidators.lessThan(max, messageFormat))

    def greaterThanOrEqualTo(min: T, messageFormat: String = "%%s must be greater than or equal to %s"): FieldDescriptor[T] =
      b.validateWith(BindingValidators.greaterThanOrEqualTo(min, messageFormat))

    def lessThanOrEqualTo(max: T, messageFormat: String = "%%s must be less than or equal to %s"): FieldDescriptor[T] =
      b.validateWith(BindingValidators.lessThanOrEqualTo(max, messageFormat))

  }

  class ValidatableGenericBinding[T](b: FieldDescriptor[T]) {
    def validate(validate: T => Boolean, messageFormat: String = "%s is invalid."): FieldDescriptor[T] = 
      b.validateWith(BindingValidators.validate(validate, messageFormat))
  }

  class ValidatableStringBinding(b: FieldDescriptor[String]) {
    def notBlank: FieldDescriptor[String] = notBlank()
    def notBlank(messageFormat: String = "%s is required"): FieldDescriptor[String] =
      b.required.validateWith(BindingValidators.nonEmptyString(messageFormat))


    def validEmail: FieldDescriptor[String] = validEmail()
    def validEmail(messageFormat: String = "%s must be a valid email address."): FieldDescriptor[String] =
    b.validateWith(BindingValidators.validEmail(messageFormat))

    def validAbsoluteUrl(allowLocalHost: Boolean, messageFormat: String = "%s must be a valid absolute url.", schemes: Seq[String] = Seq("http", "https")): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validAbsoluteUrl(allowLocalHost, messageFormat, schemes))

    def validUrl(allowLocalHost: Boolean, messageFormat: String = "%s must be a valid url.", schemes: Seq[String] = Seq("http", "https")): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validUrl(allowLocalHost, messageFormat, schemes))

    def validForFormat(regex: Regex, messageFormat: String = "%s is invalid."): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validFormat(regex, messageFormat))

    def validForConfirmation(against: Field[String], messageFormat: String = "%%s must match %s."): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validConfirmation(against, messageFormat))


    def minLength(min: Int, messageFormat: String = "%%s must be at least %s characters long."): FieldDescriptor[String] =
      b.validateWith(BindingValidators.minLength(min, messageFormat))


    def enumValue(enum: Enumeration, messageFormat: String = "%%s must be one of %s."): FieldDescriptor[String] =
      b.validateWith(BindingValidators.enumValue(enum, messageFormat))
  }

  import org.scalatra.validation.Validation


  def validate[TValue](validate: TValue => Boolean, messageFormat: String = "%s is invalid."): BindingValidator[TValue] = (s: String) => {
    _ flatMap (Validators.validate(s, messageFormat = messageFormat, validate = validate).validate(_))
  }

  def nonEmptyString: BindingValidator[String] = nonEmptyString()
  def nonEmptyString(messageFormat: String = "%s is required."): BindingValidator[String] = (s: String) => {
    _ flatMap (Validation.nonEmptyString(s, _, messageFormat))
  }

  def notNull: BindingValidator[AnyRef] = notNull()
  def notNull(messageFormat: String = "%s is required."): BindingValidator[AnyRef] = (s: String) => {
    _ flatMap (Validation.notNull(s, _, messageFormat))
  }

  def nonEmptyCollection[TResult <: Traversable[_]]: BindingValidator[TResult] = nonEmptyCollection[TResult]()
  def nonEmptyCollection[TResult <: Traversable[_]](messageFormat: String = "%s must not be empty."): BindingValidator[TResult] = (s: String) =>{
    _ flatMap (Validation.nonEmptyCollection(s, _, messageFormat))
  }

  def validEmail: BindingValidator[String] = validEmail()
  def validEmail(messageFormat: String = "%s must be a valid email address."): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validation.validEmail(s, _, messageFormat))
  }

  def validAbsoluteUrl(allowLocalHost: Boolean, messageFormat: String = "%s must be a absolute valid url.", schemes: Seq[String] = Seq("http", "https")): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.validAbsoluteUrl(s, allowLocalHost, messageFormat, schemes).validate(_))
  }

  def validUrl(allowLocalHost: Boolean, messageFormat: String = "%s must be a valid url.", schemes: Seq[String] = Seq("http", "https")): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.validUrl(s, allowLocalHost, messageFormat, schemes).validate(_))
  }

  def validFormat(regex: Regex, messageFormat: String = "%s is invalid."): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.validFormat(s, regex, messageFormat).validate(_))
  }

  def validConfirmation(against: Field[String], messageFormat: String = "%%s must match %s."): BindingValidator[String] = (s: String) =>{
    _ flatMap { Validators.validConfirmation(s, against.name, against.value | against.defaultValue, messageFormat).validate(_) }
  }

  def greaterThan[T <% Ordered[T]](min: T, messageFormat: String = "%%s must be greater than %s."): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.greaterThan(s, min, messageFormat).validate(_))
  }

  def lessThan[T <% Ordered[T]](max: T, messageFormat: String = "%%s must be less than %s."): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.lessThan(s, max, messageFormat).validate(_))
  }

  def greaterThanOrEqualTo[T <% Ordered[T]](min: T, messageFormat: String = "%%s must be greater than or equal to %s."): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.greaterThanOrEqualTo(s, min, messageFormat).validate(_))
  }

  def lessThanOrEqualTo[T <% Ordered[T]](max: T, messageFormat: String = "%%s must be less than or equal to %s."): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.lessThanOrEqualTo(s, max, messageFormat).validate(_))
  }

  def minLength(min: Int, messageFormat: String = "%%s must be at least %s characters long."): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.minLength(s, min, messageFormat).validate(_))
  }

  def oneOf[TResult](messageFormat: String = "%%s must be one of %s.", expected: Seq[TResult]): BindingValidator[TResult] = (s: String) => {
    _ flatMap (Validators.oneOf(s, messageFormat, expected).validate(_))
  }

  def enumValue(enum: Enumeration, messageFormat: String = "%%s must be one of %s."): BindingValidator[String] = 
    oneOf(messageFormat, enum.values.map(_.toString).toSeq)
}

class Field[A:Manifest](descr: FieldDescriptor[A], command: Command) {

  val name = descr.name
  def validation: FieldValidation[A] = binding.field.value.asInstanceOf[FieldValidation[A]]
  def value: Option[A] = binding.field.value.toOption.asInstanceOf[Option[A]]
  def defaultValue: A = descr.defaultValue
  def error: Option[ValidationError] = binding.field.value.fold(_.some, _=>None)
  def original = binding.original

  def binding: Binding = command.bindings(name)

  def isValid = validation.isSuccess
  def isInvalid = validation.isFailure
  
	def notes: String = descr.notes
  def description: String = descr.description
  
  def isRequired: Boolean = descr.isRequired
  def valueSource: ValueSource.Value = descr.valueSource
  def allowableValues = descr.allowableValues
  def displayName: Option[String] = descr.displayName
}
