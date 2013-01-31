package org.scalatra.swagger

import java.util.concurrent.ConcurrentHashMap
import com.thoughtworks.paranamer.{BytecodeReadingParanamer, CachingParanamer}
import java.lang.reflect.{ Constructor => JConstructor }
import collection.JavaConverters._

package object reflect {

  private[reflect] class Memo[A, R] {
    private[this] val cache = new ConcurrentHashMap[A, R]().asScala
    def apply(x: A, f: A => R): R = cache.getOrElseUpdate(x, f(x))
  }

  trait ParameterNameReader {
    def lookupParameterNames(constructor: JConstructor[_]): Traversable[String]
  }
  private[reflect] val ConstructorDefault = "init$default"
  private[reflect] val ModuleFieldName = "MODULE$"
  private[reflect] val ClassLoaders = Vector(getClass.getClassLoader)
  private[this] val paranamer = new CachingParanamer(new BytecodeReadingParanamer)

  object ParanamerReader extends ParameterNameReader {
    def lookupParameterNames(constructor: JConstructor[_]): Seq[String] =
      paranamer.lookupParameterNames(constructor)
  }

  def fail(msg: String, cause: Exception = null) = {
    if (cause != null) {
      System.err.println(msg)
      throw cause
    } else sys.error(msg)
  }
}
