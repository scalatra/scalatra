package org.scalatra.test

case class SimpleResponse(
  status: Int,
  headers: Map[String, Seq[String]],
  body: String)
