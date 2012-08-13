package org.scalatra
package util

package object conversion {
  type TypeConverter[S, T] = (S) => Option[T]
}

