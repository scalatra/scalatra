package org.scalatra.test

import java.io.{ NotSerializableException, OutputStream, ObjectOutputStream }
import javax.servlet.http.{ HttpSessionAttributeListener, HttpSessionBindingEvent }

object NullOut extends OutputStream {
  def write(b: Int) {}
}

/*
 * Taken from https://gist.github.com/3485500, Thanks @LeifWarner
 */
object SessionSerializingListener extends HttpSessionAttributeListener {
  //val oos = new ObjectOutputStream(System.out)
  val oos = new ObjectOutputStream(NullOut)

  def attributeAdded(event: HttpSessionBindingEvent) {
    serializeSession(event)
  }

  def attributeRemoved(event: HttpSessionBindingEvent) {
    serializeSession(event)
  }

  def attributeReplaced(event: HttpSessionBindingEvent) {
    serializeSession(event)
  }

  def serializeSession(event: HttpSessionBindingEvent) {
    try {
      oos.writeObject(event.getValue)
    } catch {
      case e: NotSerializableException =>
        sys.error("Can't serialize session key '" + event.getName + "' value of type " + e.getMessage)
    }
  }
}