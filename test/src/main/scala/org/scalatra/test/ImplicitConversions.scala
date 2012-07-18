package org.scalatra.test

/**
 * Contains implicit conversions for making test DSL easier
 * to use. This is included by all `Client` implementations.
 */
trait ImplicitConversions {
  implicit def stringToByteArray(str: String): Array[Byte] = str.getBytes("UTF-8")
}
