package org.scalatra.swagger

package object reflect {

  private[reflect] val ConstructorDefault = "$lessinit$greater$default"
  private[reflect] val ModuleFieldName = "MODULE$"
  private[reflect] val ClassLoaders = Vector(getClass.getClassLoader)

  def fail(msg: String, cause: Exception = null) =
    if (cause != null) {
      System.err.println(msg)
      throw cause
    } else sys.error(msg)
}
