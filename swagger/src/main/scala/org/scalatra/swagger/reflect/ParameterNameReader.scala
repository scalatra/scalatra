package org.scalatra.swagger.reflect

import java.lang.reflect.{ Constructor => JConstructor }
import com.thoughtworks.paranamer.{ BytecodeReadingParanamer, CachingParanamer }

trait ParameterNameReader {
  def lookupParameterNames(constructor: JConstructor[?]): Seq[String]
}

object ParanamerReader extends ParameterNameReader {
  private[this] val paranamer = new CachingParanamer(new BytecodeReadingParanamer)
  def lookupParameterNames(constructor: JConstructor[?]): Seq[String] =
    paranamer.lookupParameterNames(constructor).toIndexedSeq
}
