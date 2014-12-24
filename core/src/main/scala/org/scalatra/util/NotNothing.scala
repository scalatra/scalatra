package org.scalatra.util

/**
 * Nice trick from Miles Sabin using ambiguity in implicit resolution to disallow Nothing
 */
sealed trait NotNothing[A] {
  type B
}

object NotNothing {

  implicit val nothing: NotNothing[Nothing] = new NotNothing[Nothing] { type B = Unit }

  implicit def notNothing[A]: NotNothing[A] = new NotNothing[A] { type B = A }

}
