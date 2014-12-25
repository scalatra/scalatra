package org.scalatra
package servlet

import java.io.ByteArrayInputStream
import java.util.Locale
import javax.servlet.http.HttpServletRequest
import javax.servlet.{ ReadListener, ServletInputStream }

import org.mockito.Mockito._
import org.scalatest.{ FunSuite, Matchers }

class RichRequestTest extends FunSuite with Matchers {
  implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)

  test("decodes body according to the character encoding") {
    val encoding = "ISO-8859-5"
    val message = "cyrillic content: Ж, Щ, л"
    val content = message.getBytes(encoding)
    val request = createStubRequestWithContent(content, encoding)

    request.body should equal(message)
  }

  def createStubRequestWithContent(content: Array[Byte], encoding: String): HttpServletRequest = {
    val request = mock(classOf[HttpServletRequest])
    when(request.getInputStream).thenReturn(new FakeServletInputStream(content))
    when(request.getCharacterEncoding).thenReturn(encoding)
    request
  }

  test("returns locales as Seq value instead of java.util.Enumeration") {
    val request = createStubRequestWithLocales()
    request.locales should equal(Seq(new Locale("en"), new Locale("ja")))
  }

  def createStubRequestWithLocales(): HttpServletRequest = {
    val request = mock(classOf[HttpServletRequest])
    when(request.getLocales).thenReturn(new java.util.Enumeration[Locale] {
      private[this] val iter = Seq("en", "ja").iterator
      override def hasMoreElements: Boolean = iter.hasNext
      override def nextElement(): Locale = new Locale(iter.next)
    })
    request
  }
}

private[scalatra] class FakeServletInputStream(data: Array[Byte]) extends ServletInputStream {
  private[this] val backend = new ByteArrayInputStream(data)
  def read = backend.read

  def setReadListener(readListener: ReadListener) {}

  def isFinished: Boolean = true

  def isReady: Boolean = true
}
