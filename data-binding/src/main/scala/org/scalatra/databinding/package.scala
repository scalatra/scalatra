package org.scalatra

import validation.Validator
import com.fasterxml.jackson.databind.JsonNode
import scalaz._
import Scalaz._
import com.fasterxml.jackson.databind.node.MissingNode
import net.liftweb.json.JsonAST.{JValue, JNothing}

package object databinding {

  type BindingValidator[T] = (String) => Validator[T]
  implicit val jacksonZero: Zero[JsonNode] = zero(MissingNode.getInstance)
  implicit val liftJsonZero: Zero[JValue] = zero(JNothing)
}

