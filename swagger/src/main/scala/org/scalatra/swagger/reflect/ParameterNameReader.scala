package org.scalatra.swagger.reflect

import scala.collection.immutable.ArraySeq
import java.lang.reflect.Constructor as JConstructor

trait ParameterNameReader {
  def lookupParameterNames(constructor: JConstructor[?]): Seq[String]
}

object ParanamerReader extends ParameterNameReader {
  def lookupParameterNames(constructor: JConstructor[?]): Seq[String] =
    constructor match {
      case c: JConstructor[?] =>
        ArraySeq.unsafeWrapArray(
          c.getParameters().map(_.getName())
        )
      case _ =>
        Nil
    }
}
