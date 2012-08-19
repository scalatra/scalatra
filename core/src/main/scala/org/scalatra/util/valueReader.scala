package org.scalatra
package util

import conversion._
import collection.immutable
import collection.mutable
import java.util.Date
import java.text.{DateFormat, SimpleDateFormat}

trait ValueReader[S, U]  {

//  type I
  def data: S
  def read(key: String): Option[U]

//  implicit def implicitToOrig(i: I): String = i : String
}


class StringMapValueReader(val data: Map[String, String]) extends ValueReader[immutable.Map[String, String], String] {
//  type I = String
  def read(key: String): Option[String] = data get key


//  implicit val manifest: Manifest[] = Predef.manifest[String]

//  implicit def implicitToOrig(i: ): String = i : String

}

class MultiMapHeadViewValueReader[T <: MultiMapHeadView[String, String]](val data: T) extends ValueReader[T, String] {
//  type I = String
  def read(key: String): Option[String] = data get key


//  implicit val manifest: Manifest[] = Predef.manifest[String]

//  implicit def implicitToOrig(i: ): String = i : String

}

class MultiParamsValueReader(val data: MultiParams) extends ValueReader[MultiParams, Seq[String]] {
//  type I = Seq[String]
  def read(key: String): Option[Seq[String]] = data get key

//  implicit val manifest: Manifest[I] = Predef.manifest[Seq[String]]
//
//  implicit def implicitToOrig(i: I): Seq[String] = i : Seq[String]
}

trait ParamsValueReaderProperties {

  implicit def stringMapValueReader(d: immutable.Map[String, String]): ValueReader[immutable.Map[String, String], String] = new StringMapValueReader(d)
  implicit def multiMapHeadViewMapValueReader[T <: MultiMapHeadView[String, String]](d: T): ValueReader[T, String] = new MultiMapHeadViewValueReader(d)

  implicit def multiParamsValueReader(d: MultiParams): ValueReader[MultiParams, Seq[String]] = new MultiParamsValueReader(d)
}

object ParamsValueReaderProperties extends ParamsValueReaderProperties