package org.scalatra
package databinding

import util._
import conversion._
import scalaz._
import Scalaz._
import collection.immutable
import org.joda.time.DateTime
import java.util.Date

/**
* Trait that identifies a ''Command object'', i.e. a Scala class instance which fields are bound to external parameters
* taken from Scalatra' __params__ object.
*
* An usage example can be seen below:
* {{{
* class PersonForm extends Command {
*
*  import Command._
*
*  val name = bind[String]("f_name")
*  val surname = bind[String]("f_surname")
*  val age = bind[Int]("f_age")
*  val registeredOn = bind[Date]("f_reg_date" -> "yyyyMMdd")
* }
* }}}
*
* In the example above, class field ''name'' will be bound, at runtime, with a parameter named ''f_name'' and so on.
* The binding is typed and for every registered type `T` (see [[org.scalatra.command.field.ImplicitCommonFields]] for
* a list of all availables) an automatic conversion `(String) => T` will take place during binding phase.
*
* After that binding has been performed (i.e. after that [[org.scalatra.command.Command#bindTo()]] has been called)
* on a specific instance, it is possible retrieve field values as [[scalaz.Validation]], i.e.:
*
* {{{
* val form = new PersonForm
* form.doBinding(params)
* val registrationDate = form.registeredOn.value.getOrElse(new Date())
* }}}
*
*
* @author mmazzarolo
* @version 0.1
*
*/

trait Command extends BindingSyntax with ParamsValueReaderProperties {

  type CommandTypeConverterFactory[T] <: TypeConverterFactory[T]
  private[this] var preBindingActions: Seq[BindingAction] = Nil

  private[this] var postBindingActions: Seq[BindingAction] = Nil

  private[databinding] var bindings: Map[String, Binding] = Map.empty

  private[this] var _errors: Seq[Binding] = Nil

  /**
   * Check whether this command is valid.
   */
  def isValid = errors.isEmpty
  def isInvalid = errors.nonEmpty

  /**
   * Return a Map of all field command error keyed by field binding name (NOT the name of the variable in command
   * object).
   */
  def errors: Seq[Binding] = _errors

  /**
   * Perform command as afterBinding task.
   */
  afterBinding {
    _errors = bindings.values.filter(_.isInvalid).toSeq
  }


  implicit def binding2field[T:Manifest:DefaultValue:TypeConverterFactory](field: FieldDescriptor[T]): Field[T] = {
    new Field(bind(field), this)
  }

  implicit def autoBind[T:Manifest:DefaultValue:TypeConverterFactory](fieldName: String): Field[T] =
    bind[T](FieldDescriptor[T](fieldName))

  implicit def bind[T:Manifest:DefaultValue:TypeConverterFactory](field: FieldDescriptor[T]): FieldDescriptor[T] = {
    val b = Binding(field)
    bindings += b.name -> b
    field
  }

  def typeConverterBuilder[I](tc: CommandTypeConverterFactory[_]): PartialFunction[ValueReader[_, _], TypeConverter[I, _]] = {
    case r: MultiParamsValueReader => tc.resolveMultiParams.asInstanceOf[TypeConverter[I, _]]
    case r: MultiMapHeadViewValueReader[_] => tc.resolveStringParams.asInstanceOf[TypeConverter[I, _]]
    case r: StringMapValueReader => tc.resolveStringParams.asInstanceOf[TypeConverter[I, _]]
    case r => throw new BindingException("No converter found for value reader: " + r.getClass.getSimpleName)
  }

  /**
   * Add an action that will be evaluated before field binding occurs.
   */
  protected def beforeBinding(action: => Any) {
    preBindingActions = preBindingActions :+ (() => action)
  }

  /**
   * Add an action that will be evaluated after field binding has been done.
   */
  protected def afterBinding(action: => Any) {
    postBindingActions = postBindingActions :+ (() => action)
  }

  def bindTo[S, I](
            data: S,
            params: MultiParams = MultiMap.empty,
            headers: Map[String, String] = Map.empty,
            paramsOnly: Boolean = false)(implicit r: S => ValueReader[S, I], mi: Manifest[I], zi: DefaultValue[I], multiParams: MultiParams => ValueReader[MultiParams, Seq[String]]): this.type = {
    doBeforeBindingActions()

    bindings = bindings map { case (name, b) =>
      val tcf = b.typeConverterFactory
      val cv = typeConverterBuilder(tcf.asInstanceOf[CommandTypeConverterFactory[_]])(data).asInstanceOf[TypeConverter[I, b.T]]
      val fieldBinding = Binding(b.field, cv, b.typeConverterFactory)(mi, zi, b.valueManifest, b.valueZero)

      val res = this match {
        case d: ForceFromParams if d.forceFromParams.contains(name) =>
          val pv = typeConverterBuilder(tcf.asInstanceOf[CommandTypeConverterFactory[_]])(params).asInstanceOf[TypeConverter[Seq[String], b.T]]
          val paramsBinding = Binding(b.field, pv, b.typeConverterFactory)(manifest[Seq[String]], implicitly[DefaultValue[Seq[String]]], b.valueManifest, b.valueZero)
          paramsBinding(params.read(name).right.map(_ map (_.asInstanceOf[paramsBinding.S])))
        case d: ForceFromHeaders if d.forceFromHeaders.contains(name) =>
          val tc: TypeConverter[String, _] = tcf.resolveStringParams
          val headersBinding = Binding(b.field, tc.asInstanceOf[TypeConverter[String, b.T]], tcf)(manifest[String], implicitly[DefaultValue[String]], b.valueManifest, b.valueZero)
          headersBinding(Right(headers.get(name).map(_.asInstanceOf[headersBinding.S])))
        case _ if paramsOnly =>
          val pv = typeConverterBuilder(tcf.asInstanceOf[CommandTypeConverterFactory[_]])(params).asInstanceOf[TypeConverter[Seq[String], b.T]]
          val paramsBinding = Binding(b.field, pv, b.typeConverterFactory)(manifest[Seq[String]], implicitly[DefaultValue[Seq[String]]], b.valueManifest, b.valueZero)
          paramsBinding(params.read(name).right.map(_ map (_.asInstanceOf[paramsBinding.S])))
        case _ =>
          fieldBinding(data.read(name).right.map(_ map (_.asInstanceOf[fieldBinding.S])))
      }
      name -> res
    }

    bindings = bindings map { case (name, v) => name -> v.validate }
    doAfterBindingActions()
    this
  }

  private def doBeforeBindingActions() = preBindingActions.foreach(_.apply())

  private def doAfterBindingActions() = postBindingActions.foreach(_.apply())

  override def toString: String = "%s(bindings: [%s])".format(getClass.getName, bindings.mkString(", "))



}

trait ParamsOnlyCommand extends TypeConverterFactories with Command {
  type CommandTypeConverterFactory[T] = TypeConverterFactory[T]
}

trait ForceFromParams { self: Command =>

  def forceFromParams: Set[String]
}

trait ForceFromHeaders { self: Command =>

  def forceFromHeaders: Set[String]
}

