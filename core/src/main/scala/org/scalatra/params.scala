package org.scalatra

import util.conversion._
import java.util.Date
import util.{MultiMap, MapWithIndifferentAccess, MultiMapHeadView}

/**
 * Add some implicits
 */
trait ScalatraParamsImplicits {

  self: DefaultImplicitConversions =>

  type ParamsType = MultiMapHeadView[String, String] with MapWithIndifferentAccess[String]
  type MultiParamsType = MultiMap

  sealed class TypedParams(params: ParamsType) {

    def getAs[T <: Any](name: String)(implicit tc: TypeConverter[String, T]): Option[T] = params.get(name).flatMap(tc.apply(_))

    def getAs[T <: Date](nameAndFormat: (String, String)): Option[Date] = getAs(nameAndFormat._1)(stringToDate(nameAndFormat._2))

  }

  sealed class TypedMultiParams(multiParams: MultiParamsType) {

    def getAs[T <: Any](name: String)(implicit tc: TypeConverter[String, T]): Option[Seq[T]] = multiParams.get(name) map {
      s =>
        s.flatMap(tc.apply(_))
    }

    def getAs[T <: Date](nameAndFormat: (String, String)): Option[Seq[Date]] = getAs(nameAndFormat._1)(stringToDate(nameAndFormat._2))
  }

  implicit def toTypedParams(params: ParamsType) = new TypedParams(params)

  implicit def toTypedMultiParams(params: MultiParamsType) = new TypedMultiParams(params)
}

object ScalatraParamsImplicits extends ScalatraParamsImplicits with DefaultImplicitConversions