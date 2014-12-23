package org.scalatra
package util

import scala.annotation.implicitNotFound

package object conversion {

  @implicitNotFound(msg = "Cannot find a TypeConverter type class from ${S} to ${T}")
  trait TypeConverter[S, T] { def apply(s: S): Option[T] }

}

