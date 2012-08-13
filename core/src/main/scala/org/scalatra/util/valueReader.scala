package org.scalatra
package util

import conversion.{ValueHolder, ValueHolderImplicits}

trait ValueReader[S] extends ValueHolderImplicits {

  type I
  def data: S
  def read(key: String): ValueHolder[I]

}

class StringMapValueReader(val data: Map[String, String]) extends ValueReader[Map[String, String]] {
  type I = String
  def read(key: String): ValueHolder[I] = data get key
}

class MultiParamsValueReader(val data: MultiParams) extends ValueReader[MultiParams] {
  type I = Seq[String]
  def read(key: String): ValueHolder[I] = data get key
}

trait ParamsValueReaderProperties { self: ScalatraBase =>

  implicit protected def paramsValueReader(d: Map[String, String]): ValueReader[Map[String, String]] = new StringMapValueReader(d)
  implicit protected def multiParamsValueReader(d: MultiParams): ValueReader[MultiParams] = new MultiParamsValueReader(d)
}