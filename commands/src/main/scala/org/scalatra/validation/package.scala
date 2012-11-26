package org.scalatra

import util.RicherString._
package object validation {

  implicit def string2OptionField(s: String): Option[FieldName] = s.blankOption map FieldName
}
