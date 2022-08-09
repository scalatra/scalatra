package org.scalatra
package servlet

import java.io.{ ByteArrayInputStream, IOException }
import java.util.Locale

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.{ ReadListener, ServletInputStream }
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RichRequestTest extends AnyFunSuite with Matchers {
  implicit def requestWrapper(r: HttpServletRequest): RichRequest = RichRequest(r)

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
      override def nextElement(): Locale = new Locale(iter.next())
    })
    request
  }

  test("throws IOException from original HttpServletRequest") {
    val request = createStubRequestWithServletInputStreamThrowsIOException()

    assertThrows[IOException] {
      request.body
    }
  }

  private def createStubRequestWithServletInputStreamThrowsIOException(): HttpServletRequest = {
    val request = mock(classOf[HttpServletRequest])
    when(request.getInputStream).thenReturn(new ServletInputStreamThrowsIOException)

    request
  }

  test("remoteAddress for single address in X-Forwarded-For header") {
    val request = mock(classOf[HttpServletRequest])
    when(request.getHeader("X-FORWARDED-FOR")).thenReturn("1.2.3.4")
    request.remoteAddress should equal("1.2.3.4")
  }

  test("remoteAddress for multiple addresses in X-Forwarded-For header") {
    val request = mock(classOf[HttpServletRequest])
    when(request.getHeader("X-FORWARDED-FOR")).thenReturn("1.2.3.4, 5.6.7.8")
    request.remoteAddress should equal("1.2.3.4")
  }

  test("remoteAddress without X-Forwarded-For header") {
    val request = mock(classOf[HttpServletRequest])
    when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null)
    when(request.getRemoteAddr).thenReturn("0.0.0.0")
    request.remoteAddress should equal("0.0.0.0")
  }

  test("remoteAddress for blank X-Forwarded-For header") {
    val request = mock(classOf[HttpServletRequest])
    when(request.getHeader("X-FORWARDED-FOR")).thenReturn("")
    when(request.getRemoteAddr).thenReturn("0.0.0.0")
    request.remoteAddress should equal("0.0.0.0")
  }
}

private[scalatra] class FakeServletInputStream(data: Array[Byte]) extends ServletInputStream {
  private[this] val backend = new ByteArrayInputStream(data)
  def read = backend.read

  def setReadListener(readListener: ReadListener): Unit = {}

  def isFinished: Boolean = true

  def isReady: Boolean = true
}

private[scalatra] class ServletInputStreamThrowsIOException() extends ServletInputStream {
  def read = throw new IOException("Something totally bad happened!")

  def setReadListener(readListener: ReadListener): Unit = {}

  def isFinished: Boolean = true

  def isReady: Boolean = true
}
