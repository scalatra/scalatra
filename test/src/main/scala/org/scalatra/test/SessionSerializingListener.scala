package org.scalatra.test

import java.io.{ NotSerializableException, ObjectOutputStream, OutputStream }
import jakarta.servlet.http.{ HttpSessionAttributeListener, HttpSessionBindingEvent }

object NullOut extends OutputStream {
  def write(b: Int): Unit = {}
}

/*
 * Taken from https://gist.github.com/3485500, Thanks @LeifWarner
 */
object SessionSerializingListener extends HttpSessionAttributeListener {
  //val oos = new ObjectOutputStream(System.out)
  val oos = new ObjectOutputStream(NullOut)

  override def attributeAdded(event: HttpSessionBindingEvent): Unit = {
    serializeSession(event)
  }

  override def attributeRemoved(event: HttpSessionBindingEvent): Unit = {
    serializeSession(event)
  }

  override def attributeReplaced(event: HttpSessionBindingEvent): Unit = {
    serializeSession(event)
  }

  def serializeSession(event: HttpSessionBindingEvent): Unit = {
    try {
      oos.writeObject(event.getValue)
    } catch {
      case e: NotSerializableException =>
        sys.error("Can't serialize session key '" + event.getName + "' value of type " + e.getMessage)
    }
  }
}
