package org.scalatra.util

import java.text.SimpleDateFormat
import java.util.{ Date, Locale, TimeZone }

object DateUtil {
  @volatile private[this] var _currentTimeMillis: Option[Long] = None
  def currentTimeMillis = _currentTimeMillis getOrElse System.currentTimeMillis
  def currentTimeMillis_=(ct: Long) = _currentTimeMillis = Some(ct)
  def freezeTime() = _currentTimeMillis = Some(System.currentTimeMillis())
  def unfreezeTime() = _currentTimeMillis = None
  def formatDate(date: Date, format: String, timeZone: TimeZone = TimeZone.getTimeZone("GMT"), locale: Locale = Locale.ENGLISH) = {
    val df = new SimpleDateFormat(format, locale)
    df.setTimeZone(timeZone)
    df.format(date)
  }
}
