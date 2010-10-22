package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import scala.util.DynamicVariable
import javax.servlet.http.HttpServletResponse
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UrlSupportTest extends FunSuite with ShouldMatchers {
  private val urlSupport = new UrlSupport {
    def contextPath = "/context"

    val _response = new DynamicVariable[HttpServletResponse]({
      val response = mock(classOf[HttpServletResponse])
      when(response.encodeURL(anyString)).thenAnswer(new Answer[String] {
        def answer(invocation: InvocationOnMock) = invocation.getArguments.head.asInstanceOf[String]
      })
      response
    })
    def response = _response.value
  }

  import urlSupport.url

  test("a page-relative URL should not have the context path prepended") {
    url("page-relative") should equal ("page-relative")
  }

  test("a context-relative URL should have the context path prepended") {
    url("/context-relative") should equal ("/context/context-relative")
  }

  test("an absolute URL should not have the context path prepended") {
    url("http://www.example.org/") should equal ("http://www.example.org/")
  }

  test("empty params should not generate a query string") {
    url("foo", Map.empty) should equal ("foo")
  }

  test("params should be rendered as a query string") {
    url("en-to-es", Map("one" -> "uno", "two" -> "dos")) should equal ("en-to-es?one=uno&two=dos")
  }

  test("params should url encode both keys and values in UTF-8") {
    url("de-to-ru", Map("fünf" -> "пять")) should equal ("de-to-ru?f%C3%BCnf=%D0%BF%D1%8F%D1%82%D1%8C")
  }

  test("encodes URL through response") {
    val response = mock(classOf[HttpServletResponse])
    when(response.encodeURL(anyString)).thenReturn("foo;jsessionid=1234")
    urlSupport._response.withValue(response) {
      url("foo") should equal ("foo;jsessionid=1234")
    }
  }
}
