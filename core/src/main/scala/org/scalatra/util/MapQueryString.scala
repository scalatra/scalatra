package org.scalatra.util

import java.nio.charset.StandardCharsets

import collection.{SortedMap, immutable}

object MapQueryString {

  val DEFAULT_EXCLUSIONS =
    List("utm_source", "utm_medium", "utm_term", "utm_content", "utm_campaign", "sms_ss", "awesm")

  def parseString(rw: String): Map[String, List[String]] = {
    // this is probably an accident waiting to happen when people do actually mix stuff
    val semiColon = if (rw.indexOf(';') > -1) {
      rw.split(';').foldRight(Map[String, List[String]]()) { readQsPair _ }
    } else readQsPair(rw)
    val ampersand = if (rw.indexOf('&') > -1) {
      rw.split('&').foldRight(Map[String, List[String]]()) { readQsPair _ }
    } else {
      readQsPair(rw)
    }
    semiColon ++ ampersand
  }

  private def readQsPair(pair: String, current: Map[String, List[String]] = Map.empty): Map[String, List[String]] = {
    (pair.split('=').toList).map { source =>
      if (source != null && source.trim().nonEmpty) {
        UrlCodingUtils.urlDecode(source, StandardCharsets.UTF_8, plusIsSpace = true, skip = Set.empty[Int])
      } else {
        ""
      }
    } match {
      case item :: Nil => current + (item -> List[String]())
      case item :: rest =>
        if (!current.contains(item)) current + (item -> rest) else current + (item -> (rest ::: current(item)).distinct)
      case _ => current
    }
  }

  def apply(rawValue: String): MapQueryString = new MapQueryString(parseString(rawValue).toSeq, rawValue)
}
case class MapQueryString(initialValues: Seq[(String, Seq[String])], rawValue: String) {

  val uriPart: String = {
    "?" + mkString()
  }

  val empty = Map.empty[String, List[String]]

  def value: Value = Map(initialValues*)

  def normalize: MapQueryString = copy(
    SortedMap((initialValues filter (k => !MapQueryString.DEFAULT_EXCLUSIONS.contains(k._1)))*).toSeq
  )

  private def mkString(values: Value = value) = values map { case (k, v) =>
    v.map(s => "%s=%s".format(UrlCodingUtils.queryPartEncode(k), UrlCodingUtils.queryPartEncode(s))).mkString("&")
  } mkString "&"

  type Value = immutable.Map[String, Seq[String]]
}
