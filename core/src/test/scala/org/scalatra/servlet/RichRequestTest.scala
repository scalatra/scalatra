package org.scalatra
package servlet

import java.io.ByteArrayInputStream
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class RichRequestTest extends FunSuite with ShouldMatchers {
  implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)

  test("decodes body according to the character encoding") {
    val encoding = "ISO-8859-5"
    val message = "cyrillic content: Ж, Щ, л"
    val content = message.getBytes(encoding)

    val request = createStubRequest(content, encoding)

    request.body should equal (message)
  }

  def createStubRequest(content: Array[Byte], encoding: String): HttpServletRequest = {
    val request = mock(classOf[HttpServletRequest])
    when(request.getInputStream).thenReturn(new FakeServletInputStream(content))
    when(request.getCharacterEncoding).thenReturn(encoding)
    request
  }
}

private[scalatra] class FakeServletInputStream(data: Array[Byte]) extends ServletInputStream {
  private val backend = new ByteArrayInputStream(data)
  def read = backend.read
}
