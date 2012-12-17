package org.scalatra
package util

package object conversion {

  trait TypeConverter[S, T] extends ((S) => Option[T])
}

