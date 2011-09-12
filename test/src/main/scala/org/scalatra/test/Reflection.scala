package org.scalatra.test

import org.apache.commons.lang3.reflect.MethodUtils

object Reflection {
  def invokeMethod(target: AnyRef, methodName: String, args: AnyRef*): Either[Throwable, AnyRef] =
    try {
      Right(MethodUtils.invokeMethod(target, methodName, args :_*))
    }
    catch {
      case e => Left(e)
    }
}
