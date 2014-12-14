package org.scalatra.util

/**
 * Nice trick from Miles Sabin using ambiguity in implicit resolution to disallow Nothing
 */
sealed trait NotNothing[A] {
  type B
}
object NotNothing {
  implicit val nothing = new NotNothing[Nothing] { type B = Unit }
  implicit def notNothing[A] = new NotNothing[A] { type B = A }
}
