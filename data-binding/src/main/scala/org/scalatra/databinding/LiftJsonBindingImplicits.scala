package org.scalatra.databinding

import net.liftweb.json._
import org.scalatra.liftjson.LiftJsonImplicitConversions
import org.scalatra.util.conversion._
import org.joda.time.DateTime
import java.util.Date

class LiftJsonBindingImports(implicit protected val jsonFormats: Formats) extends LiftJsonBindingImplicits
trait LiftJsonBindingImplicits extends LiftJsonImplicitConversions {

  implicit def jsonToDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, DateTime] =
      safeOption(_.extractOpt[String].flatMap(df.parse))

    implicit def jsonToDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JValue, Date] =
      safeOption(_.extractOpt[String].flatMap(df.parse).map(_.toDate))

}
