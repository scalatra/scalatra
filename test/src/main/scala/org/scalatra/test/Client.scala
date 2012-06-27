package org.scalatra
package test

import scala.util.DynamicVariable
import java.io.File
import java.nio.charset.Charset

trait Client {
  private val _response = new DynamicVariable[ClientResponse](null)
  private[scalatra] val _cookies = new DynamicVariable[Seq[org.scalatra.HttpCookie]](Nil)
  private[scalatra] val _useSession = new DynamicVariable(false)

  lazy val charset = Charset.defaultCharset()

  def start() {}

  def stop() {}

  /**
   * Returns the current response within the scope of the submit method.
   */
  def response: ClientResponse = _response.value

  def cookies = _cookies.value

  def useSession = _useSession.value

  def body = response.body

  def headers = response.headers

  def status = response.status

  protected def withResponse[A](res: ClientResponse)(f: => A): A = {
    if (_useSession.value && res.cookies.size > 0)
      _cookies.value = res.cookies.values.toSeq
    _response.withValue(res) {
      f
    }
  }

  def submit[A](
                 method: String,
                 uri: String,
                 params: Iterable[(String, String)] = Map.empty,
                 headers: Map[String, String] = Map.empty,
                 files: Seq[File] = Seq.empty,
                 body: String = null)(f: => A): A

  def get[A](uri: String)(f: => A): A = submit("GET", uri)(f)

  def get[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)

  def get[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("GET", uri, params, headers)(f)

  def head[A](uri: String)(f: => A): A = submit("HEAD", uri)(f)

  def head[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    head(uri, params, Map[String, String]())(f)

  def head[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("HEAD", uri, params, headers)(f)

  def post[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    post(uri, params, Map.empty[String, String])(f)

  def post[A](uri: String, files: Iterable[File])(f: => A): A =
    post(uri, Seq.empty, Map[String, String](), files.toSeq)(f)

  def post[A](uri: String, files: Seq[File], headers: Map[String, String])(f: => A): A =
    post(uri, Seq.empty, headers, files)(f)

  def post[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String])(f: => A): A =
    post(uri, params, headers, Seq.empty)(f)

  def post[A](uri: String, params: Iterable[(String, String)], files: Seq[File])(f: => A): A =
    post(uri, params, Map[String, String](), files)(f)

  def post[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String], files: Seq[File])(f: => A): A =
    submit("POST", uri, params, defaultWriteContentType(files) ++ headers, files)(f)

  def post[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("POST", uri, headers = defaultWriteContentType(Seq.empty) ++ headers, body = body)(f)

  def put[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    put(uri, params, Map.empty[String, String])(f)

  def put[A](uri: String, files: Iterable[File])(f: => A): A =
    put(uri, Seq.empty, Map[String, String](), files.toSeq)(f)

  def put[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String])(f: => A): A =
    put(uri, params, headers, Seq.empty)(f)

  def put[A](uri: String, params: Iterable[(String, String)], files: Seq[File])(f: => A): A =
    put(uri, params, Map[String, String](), files)(f)

  def put[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String], files: Seq[File])(f: => A): A =
    submit("PUT", uri, params, defaultWriteContentType(files) ++ headers, files)(f)

  def put[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A) =
    submit("PUT", uri, headers = defaultWriteContentType(Seq.empty) ++ headers, body = body)(f)

  def deleteReq[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("DELETE", uri, params, headers)(f)

  def options[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("OPTIONS", uri, params, headers)(f)

  def trace[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("TRACE", uri, params, headers)(f)

  def connect[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("CONNECT", uri, params, headers)(f)

  def patch[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    patch(uri, params, Map.empty[String, String])(f)

  def patch[A](uri: String, files: Iterable[File])(f: => A): A =
    patch(uri, Seq.empty, Map[String, String](), files.toSeq)(f)

  def patch[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String])(f: => A): A =
    patch(uri, params, headers, Seq.empty)(f)

  def patch[A](uri: String, params: Iterable[(String, String)], files: Seq[File])(f: => A): A =
    patch(uri, params, Map.empty[String, String], files)(f)

  def patch[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String], files: Seq[File])(f: => A): A =
    submit("PATCH", uri, params, defaultWriteContentType(files) ++ headers, files)(f)

  def patch[A](uri: String, body: String, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("PATCH", uri, headers = defaultWriteContentType(Seq.empty) ++ headers, body = body)(f)

  private[test] def defaultWriteContentType(files: Seq[File]) = {
    val value = if (files.nonEmpty) "multipart/form-data" else "application/x-www-form-urlencoded; charset=utf-8"
    Map("Content-Type" -> value)
  }

  def session[A](f: => A): A = {
    _cookies.withValue(Nil) {
      _useSession.withValue(true)(f)
    }
  }
}