package org.scalatra
package util

import conversion.{ValueHolder, ValueHolderImplicits}
import collection.immutable
import collection.mutable

trait ValueReader[S]  {

  type I
  def data: S
  def read(key: String): Option[I]

}

class StringMapValueReader(val data: Map[String, String]) extends ValueReader[immutable.Map[String, String]] {
  type I = String
  def read(key: String): Option[I] = data get key
}

class MultiParamsValueReader(val data: MultiParams) extends ValueReader[MultiParams] {
  type I = Seq[String]
  def read(key: String): Option[I] = data get key
}

trait ParamsValueReaderProperties {

  implicit def paramsValueReader(d: immutable.Map[String, String]): ValueReader[immutable.Map[String, String]] = new StringMapValueReader(d)
  implicit def multiParamsValueReader(d: MultiParams): ValueReader[MultiParams] = new MultiParamsValueReader(d)
}

object ParamsValueReaderProperties extends ParamsValueReaderProperties