package org.scalatra
package databinding

import util._
import scalaz._
import Scalaz._
import collection.immutable

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
* After that binding has been performed (i.e. after that [[org.scalatra.command.Command#doBinding()]] has been called)
* on a specific instance, it is possible retrieve field values as [[scala.Option]], i.e.:
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
trait Command extends CommandBindingSyntax with ValueReaderZeroes {

  self: Command =>


  protected implicit val thisCommand: Command = this
  type Params = Map[String, String]

  type BindingAction = () => Any

  private[this] var preBindingActions: Seq[BindingAction] = Nil

  private[this] var postBindingActions: Seq[BindingAction] = Nil

  private[scalatra] var bindings: Map[String, Binding[_]] = Map.empty

  def replace[T](b: Binding[T]): Binding[T] = {
    bindings += b.name -> b
    b
  }

  def apply(name: String): Binding[String] = {
    println("looking for binding with name: " + name + " in:")
    println(bindings)
    val r = bindings(name)
    println("found: " + r)
    r.asInstanceOf[Binding[String]]
  }

  /**
   * Create a binding with the given [[org.scalatra.command.field.Field]].
   */
  def bind[T: Manifest](field: String): Binding[T] = replace(CommandBinding(field))
  def bind[T: Manifest](b: Binding[T]): Binding[T] = {
    val converted: Binding[T] = b match {
      case c: ValidatedBinding[_, _] => new ValidatedCommandBinding(c.binding.asInstanceOf[ValidatableBinding[_, T]])
      case _: ValidatableBinding[_, _] =>
        val c = b.asInstanceOf[ValidatableBinding[_, T]]
        BoundCommandBinding(c.original, c.value, c.binding)
      case _: CommandBinding[_] | _: BoundCommandBinding[_, _] | _: ValidatedCommandBinding[_, _] => b
      case _: Binding[_] => new CommandBinding(b.name, b.validators, b.defaultValue)

    }
    replace(converted)
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

  /**
   * Bind all registered [[org.scalatra.command.Binding]] with values
   * taken from Scalatra [[org.scalatra.ScalatraBase.params]].
   *
   * Also execute any ''before'' and ''after'' action eventually registered.
   *
   */
   def bindTo(
          data: Map[String, String],
          params: MultiParams = MultiMap.empty,
          headers: Map[String, String] = Map.empty,
          paramsOnly: Boolean = false)(
          implicit
            mf: Manifest[Map[String, String]],
            reader: Map[String, String] => ValueReader[Map[String, String]],
            multiParams: MultiParams => ValueReader[MultiParams]): this.type = {
    doBeforeBindingActions
    val boundBindings: Map[String, Binding[_]] = bindings map { case (name, binding) =>
      val b = binding.asInstanceOf[Binding[String]]
      println("binding: " + name)
      val d = data.read(name)
      println("The data: " + d)
      val r = b(data.read(name).map(_.asInstanceOf[String]))
      println("The binding: " + r)
      name -> r
//      this match {
//        case d: ForceFromParams if d.namesToForce.contains(b.name) => b(params.read(b.name).flatMap(_.asInstanceOf[Seq[String]].headOption))
//        case d: ForceFromHeaders if d.namesToForce.contains(b.name) => b(headers.get(binding.name))
//        case _ if paramsOnly => b(params.read(b.name).map(_.asInstanceOf[Seq[String]].headOption))
//        case _ => b(data.read(b.name).map(_.asInstanceOf[String]))
//      }
    }
    bindings = boundBindings
    doAfterBindingActions
    this
  }

//  private def bindData[T](binding: Binding[_])(implicit reader: ValueReader[T]) = {
//    reader.read(binding.name)
//  }

  private def doBeforeBindingActions = preBindingActions.foreach(_.apply())

  private def doAfterBindingActions = postBindingActions.foreach(_.apply())

  override def toString: String = "%s(bindings: [%s])".format(getClass.getName, bindings.mkString(", "))
}

trait ForceFromParams { self: Command =>

  def namesToForce: Set[String]
}

trait ForceFromHeaders { self: Command =>

  def namesToForce: Set[String]
}

class CastingMultiParamsValueReader(rdr: ValueReader[MultiParams]) {
  def readAndCast(key: String): Option[Seq[String]] = rdr.read(key).map(_.asInstanceOf[Seq[String]])
}
class CastingParamsValueReader(rdr: ValueReader[Map[String, String]]) {
  def readAndCast(key: String): Option[String] = rdr.read(key).map(_.asInstanceOf[String])
}
trait ValueReaderZeroes extends ParamsValueReaderProperties {
  implicit def zeroParamsValue: Zero[ValueReader[immutable.Map[String, String]]#I] =
    zero(Zero.StringZero.zero.asInstanceOf[ValueReader[immutable.Map[String, String]]#I])

  implicit def zeroMultiParamsValue: Zero[ValueReader[MultiParams]#I] =
    zero(Zero.TraversableZero[Seq[String]].zero.asInstanceOf[ValueReader[MultiParams]#I])
  implicit def richerParamsValueReader(rdr: ValueReader[immutable.Map[String, String]]): CastingParamsValueReader =
    new CastingParamsValueReader(rdr)
  implicit def richerMultiParamsValueReader(rdr: ValueReader[MultiParams]): CastingMultiParamsValueReader =
    new CastingMultiParamsValueReader(rdr)
}
