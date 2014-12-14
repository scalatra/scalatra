package org.scalatra.swagger

import java.util.concurrent.ConcurrentHashMap
import com.thoughtworks.paranamer.{ BytecodeReadingParanamer, CachingParanamer }
import java.lang.reflect.{ Constructor => JConstructor }
import collection.JavaConverters._

package object reflect {

  private[reflect] class Memo[A, R] {
    private[this] val cache = new ConcurrentHashMap[A, R](1500, 1, 1)
    def apply(x: A, f: A => R): R = {
      if (cache.containsKey(x))
        cache.get(x)
      else {
        val v = f(x)
        replace(x, v)
      }
    }

    def replace(x: A, v: R): R = {
      cache.put(x, v)
      v
    }
  }

  trait ParameterNameReader {
    def lookupParameterNames(constructor: JConstructor[_]): Seq[String]
  }
  private[reflect] val ConstructorDefault = "$lessinit$greater$default"
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
