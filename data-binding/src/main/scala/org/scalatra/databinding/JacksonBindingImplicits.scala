package org.scalatra
package databinding

import org.scalatra.jackson._
import util.conversion._
import org.joda.time.DateTime
import java.util.Date
import com.fasterxml.jackson.databind.JsonNode

trait JacksonBindingImplicits extends JacksonImplicitConversions {
  implicit def jsonToDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JsonNode, DateTime] =
    safeOption(s => if (s.isTextual()) df.parse(s.asText()) else None)

  implicit def jsonToDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[JsonNode, Date] =
    safeOption(s => if (s.isTextual()) df.parse(s.asText()).map(_.toDate) else None)
}

object JacksonBindingImplicits extends JacksonBindingImplicits

trait JacksonCommand extends Command with JacksonBindingImplicits { self: Command =>

}
