package org.scalatra
package databinding

import org.specs2.mutable.Specification
import json.JsonSupport
import org.scalatra.validation.ValidationSupport

trait BindingTemplate { self: Command =>


  val upperCaseName = bind[String]("name").transform(_.toUpperCase)

  val lowerCaseSurname = asString("surname").transform(_.toLowerCase)

  val age = asType[Int]("age") // explicit

  val cap: Binding[Int] = "cap" // implicit

}

class WithBinding extends Command with BindingTemplate {

  val a = bind(upperCaseName)

  val lower = bind(lowerCaseSurname)
}


class CommandSpec extends Specification {

  import org.scalatra.util.ParamsValueReaderProperties._
//  implicit val formats: Formats = DefaultFormats
  "The 'Command' trait" should {

    "bind and register a 'Binding[T]' instance" in {
      val form = new WithBinding
      form.a must beAnInstanceOf[Binding[String]]
      form.a must_== form.upperCaseName
    }

    "have unprocessed binding values set to 'None'" in {
      val form = new WithBinding
      form.a.value must_== None
      form.lower.value must_== None
    }

    "doBinding 'params' Map and bind matching values to specific " in {
      val form = new WithBinding
      val params = Map("name" -> "John", "surname" -> "Doe")
      val bound = form.bindTo(params)
      bound.a.value must_== Some(params("name").toUpperCase)
      bound.lower.value must_== Some(params("surname").toLowerCase)
    }

//    "provide pluggable actions processed 'BEFORE' binding " in {
//      import System._
//
//      trait PreBindAction extends WithBinding {
//
//        var timestamp: Long = _
//
//        beforeBinding {
//          a.getClass.getMethod("original") must beNull
//          timestamp = currentTimeMillis()-1
//        }
//
//
//      }
//
//      val form = new WithBinding with PreBindAction
//      val params = Map("name" -> "John", "surname" -> "Doe")
//
//      form.timestamp must_== 0L
//
//      val bound = form.bindTo(params)
//
//      bound.timestamp must be_<(currentTimeMillis())
//      bound.a.value must beSome(params("name").toUpperCase)
////      bound.a.asBound.original must_== params("name")
//    }
//
//    "provide pluggable actions processed 'AFTER' binding " in {
//
//      trait AfterBindAction extends WithBinding {
//
//        private var _fullname: String = _
//
//        def fullName: Option[String] = Option {
//          _fullname
//        }
//
//        afterBinding {
//          _fullname = a.value.get + " " + lower.value.get
//        }
//      }
//
//      val params = Map("name" -> "John", "surname" -> "Doe")
//      val form = new WithBinding with AfterBindAction
//
//      form.fullName must beNone
//
//      form.bindTo(params)
//
//      form.fullName must beSome[String]
//      form.fullName.get must_== params("name").toUpperCase + " " + params("surname").toLowerCase
//    }
  }
}

//import org.scalatra.test.specs2._
//
//
//class CommandSample extends Command {
//  var binded = false
//
//  afterBinding {
//    binded = true
//  }
//}
//
//class CommandSupportSpec extends Specification {
//
//  class ScalatraPage extends ScalatraFilter with JsonSupport with CommandSupport
////  implicit val formats: Formats = DefaultFormats
//  "The CommandSupport trait" should {
//
//    "provide a convention for request keys with commandRequestKey[T]" in {
//
//      val page = new ScalatraPage
//      val key = page.commandRequestKey[CommandSample]
//      key must_== "_command_" + classOf[CommandSample].getName
//
//    }
//
//    "look into request for existent command objects with commandOption[T]" in {
//
//      import collection.mutable._
//
//      val mockRequest: Map[String, AnyRef] = Map.empty
//
//      val page = new ScalatraPage {
//        override private[databinding] def requestProxy = mockRequest
//      }
//
//      page.commandOption[CommandSample] must beNone
//
//      val instance = new CommandSample
//      val key = page.commandRequestKey[CommandSample]
//      mockRequest(key) = instance
//
//      page.commandOption[CommandSample] must beSome[CommandSample]
//      page.commandOption[CommandSample].get must_== instance
//    }
//
//    "create, bind and store in request new commands with command[T]" in {
//
//      import collection.mutable._
//
//      val mockRequest: Map[String, AnyRef] = Map.empty
//
//      val page = new ScalatraPage {
//        override private[databinding] def requestProxy = mockRequest
//      }
//
//      val command = page.command[CommandSample]
//
//      val key = page.commandRequestKey[CommandSample]
//      mockRequest(key) must beTheSameAs(command)
//
//      command.binded must beTrue
//    }
//  }
//
//  "provide a RouteMatcher that match valid commands with ifValid[T]" in {
//
//    class ValidatedCommand extends CommandSample with ValidationSupport
//
//    val page = new ScalatraPage
//
//    val matcher:RouteMatcher = page.ifValid[ValidatedCommand]
//
//    matcher must not(beNull[RouteMatcher])
//  }
//}
//
//
