package org.scalatra
package util

import scala.collection.immutable
import scala.util.control.Exception.allCatch

trait ValueReader[S, U] {
  def data: S
  def read(key: String): Either[String, Option[U]]
}

class StringMapValueReader(val data: Map[String, String]) extends ValueReader[immutable.Map[String, String], String] {
  def read(key: String): Either[String, Option[String]] =
    allCatch.withApply(t => Left(t.getMessage)) { Right(data get key) }
}

class MultiMapHeadViewValueReader[T <: MultiMapHeadView[String, String]](val data: T) extends ValueReader[T, String] {
  def read(key: String): Either[String, Option[String]] =
    allCatch.withApply(t => Left(t.getMessage)) { Right(data get key) }
}

class MultiParamsValueReader(val data: MultiParams) extends ValueReader[MultiParams, Seq[String]] {
  def read(key: String): Either[String, Option[Seq[String]]] =
    allCatch.withApply(t => Left(t.getMessage)) { Right(data get key) }
}

trait ParamsValueReaderProperties {
  implicit def stringMapValueReader(d: immutable.Map[String, String]): ValueReader[immutable.Map[String, String], String] = new StringMapValueReader(d)
  implicit def multiMapHeadViewMapValueReader[T <: MultiMapHeadView[String, String]](d: T): ValueReader[T, String] = new MultiMapHeadViewValueReader(d)
  implicit def multiParamsValueReader(d: MultiParams): ValueReader[MultiParams, Seq[String]] = new MultiParamsValueReader(d)
}

object ParamsValueReaderProperties extends ParamsValueReaderProperties