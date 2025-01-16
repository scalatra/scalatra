package org.scalatra
package json

import org.json4s.*
import org.scalatra.util.RicherString.*
import org.scalatra.util.ValueReader

import scala.util.control.Exception.allCatch

object JsonValueReader {
  private val separatorBeginning = "."
  private val separatorEnd       = ""
}

class JsonValueReader(val data: JValue) extends ValueReader[JValue, JValue] {
  //  type I = T
  import JsonValueReader.*

  def read(key: String): Either[String, Option[JValue]] =
    allCatch.withApply(t => Left(t.getMessage)) { Right(readPath(key)) }

  protected def readPath(path: String, subj: JValue = data): Option[JValue] = {
    val partIndex    = path.indexOf(separatorBeginning)
    val (part, rest) = if (path.indexOf(separatorBeginning) > -1) path.splitAt(partIndex) else (path, "")
    val realRest = if (rest.nonEmpty) {
      if (separatorEnd.nonBlank) {
        if (rest.size > 1) rest.substring(2) else rest.substring(1)
      } else rest.substring(1)
    } else rest
    if (realRest.isEmpty) {
      get(part, subj)
    } else {
      get(part, subj) flatMap (readPath(realRest, _))
    }
  }

  protected def get(path: String, subj: JValue): Option[JValue] = {
    val jv = subj \ path
    jv match {
      case JNothing => None
      case o        => Some(o)
    }
  }
}
trait JsonValueReaderProperty[T] { self: JsonMethods[T] =>

  implicit protected def jsonFormats: Formats
  protected implicit def jsonValueReader(d: JValue): JsonValueReader = new JsonValueReader(d)
}
