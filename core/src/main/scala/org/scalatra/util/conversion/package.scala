package org.scalatra
package util

package object conversion {

  trait TypeConverter[S, T] { def apply(s: S): Option[T] }

}

