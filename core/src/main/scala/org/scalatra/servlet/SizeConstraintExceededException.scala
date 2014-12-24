package org.scalatra.servlet

class SizeConstraintExceededException(message: String, t: Throwable)
  extends Exception(message, t)

