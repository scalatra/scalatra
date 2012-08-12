package org.scalatra
package util

package object conversion {
  type TypeConverter[T] = (String) => Option[T]
}

