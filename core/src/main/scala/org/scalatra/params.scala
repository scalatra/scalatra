package org.scalatra

import util.conversion._
import java.util.Date
import util.{ MultiMap, MapWithIndifferentAccess, MultiMapHeadView }

/**
 * Add some implicits
 */
trait ScalatraParamsImplicits {

  self: DefaultImplicitConversions =>

  sealed class TypedParams(params: Params) {

    def getAs[T <: Any](name: String)(implicit tc: TypeConverter[String, T]): Option[T] = params.get(name).flatMap(tc(_))

    def getAs[T <: Date](nameAndFormat: (String, String)): Option[Date] = getAs(nameAndFormat._1)(stringToDate(nameAndFormat._2))

    def as[T <: Any](name: String)(implicit tc: TypeConverter[String, T]): T =
      getAs[T](name) getOrElse (throw new ScalatraException("Key %s could not be found.".format(name)))

    def as[T <: Date](nameAndFormat: (String, String)): Date =
      getAs[T](nameAndFormat) getOrElse (throw new ScalatraException("Key %s could not be found.".format(nameAndFormat._1)))

    def getAsOrElse[T <: Any](name: String, default: => T)(implicit tc: TypeConverter[String, T]): T =
      getAs[T](name).getOrElse(default)

    def getAsOrElse(nameAndFormat: (String, String), default: => Date)(implicit tc: TypeConverter[String, Date]): Date =
      getAs[Date](nameAndFormat).getOrElse(default)

  }

  sealed class TypedMultiParams(multiParams: MultiParams) {

    def getAs[T <: Any](name: String)(implicit tc: TypeConverter[String, T]): Option[Seq[T]] = multiParams.get(name) map {
      s =>
        s.flatMap(tc.apply(_))
    }

    def getAs[T <: Date](nameAndFormat: (String, String)): Option[Seq[Date]] = getAs(nameAndFormat._1)(stringToDate(nameAndFormat._2))

    def as[T <: Any](name: String)(implicit tc: TypeConverter[String, T]): Seq[T] =
      getAs[T](name) getOrElse (throw new ScalatraException("Key %s could not be found.".format(name)))

    def as[T <: Date](nameAndFormat: (String, String)): Seq[Date] =
      getAs[T](nameAndFormat) getOrElse (throw new ScalatraException("Key %s could not be found.".format(nameAndFormat._1)))

    def getAsOrElse[T <: Any](name: String, default: => Seq[T])(implicit tc: TypeConverter[String, T]): Seq[T] =
      getAs[T](name).getOrElse(default)

    def getAsOrElse(nameAndFormat: (String, String), default: => Seq[Date])(implicit tc: TypeConverter[String, Date]): Seq[Date] =
      getAs[Date](nameAndFormat).getOrElse(default)

  }

  implicit def toTypedParams(params: Params) = new TypedParams(params)

  implicit def toTypedMultiParams(params: MultiParams) = new TypedMultiParams(params)
}

object ScalatraParamsImplicits extends ScalatraParamsImplicits with DefaultImplicitConversions