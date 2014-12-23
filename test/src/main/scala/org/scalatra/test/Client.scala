package org.scalatra.test

import java.net.URLEncoder.encode

import scala.util.DynamicVariable

trait Client extends ImplicitConversions {
  private val _response = new DynamicVariable[ClientResponse](null)

  def session[A](f: => A): A

  /**
   * Returns the current response within the scope of the submit method.
   */
  def response: ClientResponse = _response.value

  def body = response.body

  def bodyBytes = response.bodyBytes

  def status = response.statusLine.code

  def header = response.header

  protected def withResponse[A](res: ClientResponse)(f: => A): A = {
    _response.withValue(res) { f }
  }

  def submit[A](
    method: String,
    uri: String,
    queryParams: Iterable[(String, String)] = Seq.empty,
    headers: Iterable[(String, String)] = Seq.empty,
    body: Array[Byte] = null)(f: => A): A

  protected def submitMultipart[A](
    method: String,
    uri: String,
    params: Iterable[(String, String)] = Seq.empty,
    headers: Iterable[(String, String)] = Seq.empty,
    files: Iterable[(String, Any)] = Map.empty)(f: => A): A

  def get[A](uri: String)(f: => A): A = submit("GET", uri) { f }
  def get[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def get[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("GET", uri, params, headers) { f }

  def head[A](uri: String)(f: => A): A = submit("HEAD", uri) { f }
  def head[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def head[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("HEAD", uri, params, headers) { f }

  def post[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    post(uri, params)(f)
  def post[A](uri: String, params: Iterable[(String, String)])(f: => A): A =
    post(uri, params, Map[String, String]())(f)
  def post[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String])(f: => A): A =
    post(uri, toQueryString(params).getBytes("UTF-8"), Seq("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def post[A](uri: String, body: Array[Byte] = Array(), headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("POST", uri, Seq.empty, headers, body) { f }
  def post[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, Any)])(f: => A): A =
    post(uri, params, files, Seq.empty) { f }
  def post[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, Any)], headers: Iterable[(String, String)])(f: => A): A =
    submitMultipart("POST", uri, params, headers, files) { f }

  def put[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    put(uri, params)(f)
  def put[A](uri: String, params: Iterable[(String, String)])(f: => A): A =
    put(uri, params, Map[String, String]())(f)
  def put[A](uri: String, params: Iterable[(String, String)], headers: Map[String, String])(f: => A): A =
    put(uri, toQueryString(params).getBytes("UTF-8"), Seq("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def put[A](uri: String, body: Array[Byte] = Array(), headers: Iterable[(String, String)] = Seq.empty)(f: => A) =
    submit("PUT", uri, Seq.empty, headers, body) { f }
  def put[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, Any)])(f: => A): A =
    put(uri, params, files, Seq.empty) { f }
  def put[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, Any)], headers: Iterable[(String, String)])(f: => A): A =
    submitMultipart("PUT", uri, params, headers, files) { f }

  def delete[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("DELETE", uri, params, headers) { f }

  def options[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("OPTIONS", uri, params, headers) { f }

  def trace[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("TRACE", uri, params, headers) { f }

  def connect[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("CONNECT", uri, params, headers) { f }

  def patch[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    patch(uri, params)(f)
  def patch[A](uri: String, params: Iterable[(String, String)])(f: => A): A =
    patch(uri, params, Map[String, String]())(f)
  def patch[A](uri: String, params: Iterable[(String, String)], headers: Iterable[(String, String)])(f: => A): A =
    patch(uri, toQueryString(params).getBytes("UTF-8"), Seq("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def patch[A](uri: String, body: Array[Byte] = Array(), headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    submit("PATCH", uri, Seq.empty, headers, body) { f }

  private[test] def toQueryString(params: Traversable[(String, String)]) =
    params.map(t => List(t._1, t._2).map(encode(_, "UTF-8")).mkString("=")).mkString("&")

}
