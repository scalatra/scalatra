package org.scalatra.test

import scala.util.DynamicVariable
import java.net.URLEncoder.encode
import dispatch._
import org.apache.http.{HttpEntity, HttpResponse}
import java.io.File

trait DispatchClient extends Client {
  type Response = SimpleResponse

  def baseUrl: String

  lazy val http: Http = Http

  def submit[A](
    method: String,
    uri: String,
    queryParams: Iterable[(String, String)] = Map.empty,
    headers: Map[String, String] = Map.empty,
    body: String = null)(f: => A): A =
  {
    var req = url(baseUrl + uri) <<? queryParams <:< headers
    Option(body) foreach { str => req <<<= body }
    def headerMap(res: HttpResponse) =
      (Map[String, Seq[String]]().withDefaultValue(Seq()) /: res.getAllHeaders) {
        (m, h) => m + (h.getName -> (m(h.getName) :+ h.getValue))
      }
    val res = http x (req as_str) { case (status, res, _, body) =>
      SimpleResponse(status, headerMap(res), body())
    }
    withResponse(res) { f }
  }

  protected def submitMultipart[A](
      method: String,
      uri: String,
      params: Iterable[(String, String)] = Map.empty,
      headers: Map[String, String] = Map.empty,
      files: Iterable[(String, File)] = Map.empty
    )(f: => A): A = {
    throw new UnsupportedOperationException()
  }

  def status = response.status

  def headers = response.headers

  def body = response.body
}
