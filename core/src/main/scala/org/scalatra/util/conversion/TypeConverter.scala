package org.scalatra.util.conversion

import scala.annotation.implicitNotFound

@implicitNotFound(msg =
  "Cannot find a TypeConverter type class from ${S} to ${T}"
)
trait TypeConverter[S, T] { def apply(s: S): Option[T] }
