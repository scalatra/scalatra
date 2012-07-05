package org.scalatra
package util

import collection.Map

class HeaderWithQParser(headers: Map[String, String]) {

  def parse(key: String): List[String] = headers get key map parseHeaderLine getOrElse Nil

  private val parseHeaderLine =
    (splitHeaderLine _) andThen (parseValues _) andThen (sortByQuality _) andThen (extractValues _)

  private def splitHeaderLine(line: String): Array[String] = line.split(",").map(_.trim)

  private def parseValues(values: Array[String]): List[(Int, List[String])] =
    (values.foldLeft(Map.empty[Int, List[String]]) { (acc, f) =>
      val parts = f.split(";").map(_.trim)
      val i = if (parts.size > 1) {
        val pars = parts(1).split("=").map(_.trim).grouped(2).map(a => a(0) -> a(1)).toSeq
        val a = Map(pars:_*)
        (a.get("q").map(_.toDouble).getOrElse(1.0) * 10).round.toInt
      } else 10
      acc + (i -> (parts(0) :: acc.get(i).getOrElse(List.empty)))
    }).toList

  private def sortByQuality(parsed: List[(Int, List[String])]) = parsed sortWith ((kv1, kv2) => kv1._1 > kv2._1)

  private def extractValues(sorted: List[(Int, List[String])]) = sorted flatMap (_._2.reverse)
}
