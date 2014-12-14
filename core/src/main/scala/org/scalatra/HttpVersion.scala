package org.scalatra

abstract class HttpVersion(val protocolName: String, val majorVersion: Int, val minorVersion: Int, val keepAliveDefault: Boolean) extends Ordered[HttpVersion] {

  val text = protocolName + '/' + majorVersion + '.' + minorVersion

  override def toString = text

  override def hashCode() = protocolName.## * 31 + majorVersion.## * 31 + minorVersion

  override def equals(obj: Any) = obj match {
    case m: HttpVersion => protocolName == m.protocolName && majorVersion == m.majorVersion && minorVersion == m.minorVersion
    case _ => false
  }

  def compare(that: HttpVersion) = {
    val v = protocolName.compareTo(that.protocolName)
    if (v != 0) v
    else {
      val vv = majorVersion - that.majorVersion
      if (vv != 0) vv
      else minorVersion - that.minorVersion
    }
  }
}

object Http10 extends HttpVersion("HTTP", 1, 0, false)
object Http11 extends HttpVersion("HTTP", 1, 1, true)
