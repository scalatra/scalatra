package org.scalatra.util

import java.text.SimpleDateFormat
import java.util.{ Date, Locale, TimeZone }

object DateUtil {

  @volatile private[this] var _currentTimeMillis: Option[Long] = None

  def currentTimeMillis: Long = _currentTimeMillis getOrElse System.currentTimeMillis

  def currentTimeMillis_=(ct: Long): Unit = _currentTimeMillis = Some(ct)

  def freezeTime(): Unit = _currentTimeMillis = Some(System.currentTimeMillis())

  def unfreezeTime(): Unit = _currentTimeMillis = None

  def formatDate(
    date: Date,
    format: String,
    timeZone: TimeZone = TimeZone.getTimeZone("GMT"),
    locale: Locale = Locale.ENGLISH): String = {
    val df = new SimpleDateFormat(format, locale)
    df.setTimeZone(timeZone)
    df.format(date)
  }

}
