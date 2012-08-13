package org.scalatra
package json

import org.scalatra.util.ValueReader
import util.RicherString._

abstract class JsonValueReader[T](val data: T) extends ValueReader[T] {
  type I = T

  private val separator = new {
    val beginning = "."
    val end = ""
  }

  def read(key: String): Option[I] = readPath(key)

  protected def readPath(path: String, subj: T = data): Option[T] = {
    val partIndex = path.indexOf(separator.beginning)
    val (part, rest) = if (path.indexOf(separator.beginning) > -1) path.splitAt(partIndex) else (path, "")
    val realRest = if (rest.nonEmpty) {
      if (separator.end.nonBlank) {
        if (rest.size > 1) rest.substring(2) else rest.substring(1)
      } else rest.substring(1)
    } else rest
    if (realRest.isEmpty) {
      get(part, subj)
    } else {
      get(part, subj) flatMap (readPath(realRest, _))
    }
  }

  protected def get(path: String, subj: T): Option[T]
}
trait JsonValueReaderProperty { self: JsonSupport =>

  protected implicit def jsonValueReader(d: JsonType): JsonValueReader[JsonType]
}

