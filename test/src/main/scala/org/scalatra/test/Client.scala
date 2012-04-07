package org.scalatra.test

import scala.util.DynamicVariable
import java.net.URLEncoder.encode
import java.io.File

trait Client {
  type Response >: Null

  private val _response = new DynamicVariable[Response](null)

  /**
   * Returns the current response within the scope of the submit method.
   */
  def response: Response = _response.value

  protected def withResponse[A](res: Response)(f: => A): A =
   _response.withValue(res) { f }

  def submit[A](
    method: String,
    uri: String,
    queryParams: Iterable[(String, String)] = Map.empty,
    headers: Map[String, String] = Map.empty,
    body: String = null)(f: => A): A

  protected def submitMultipart[A](
    method: String,
    uri: String,
    params: Iterable[(String, String)] = Map.empty,
    headers: Map[String, String] = Map.empty,
    files: Iterable[(String, File)] = Map.empty
  )(f: => A): A

  def get[A](uri: String)(f: => A): A = submit("GET", uri) { f }
  def get[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def get[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("GET", uri, params, headers) { f }

  def head[A](uri: String)(f: => A): A = submit("HEAD", uri) { f }
  def head[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    get(uri, params, Map[String, String]())(f)
  def head[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("HEAD", uri, params, headers) { f }

  def post[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    post(uri, params)(f)
  def post[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    post(uri, params, Map[String, String]())(f)
  def post[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    post(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def post[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("POST", uri, Seq.empty, headers, body) { f }
  def post[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, File)])(f: => A): A =
    post(uri, params, files, Map[String, String]()) { f }
  def post[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, File)], headers: Map[String, String])(f: => A): A =
    submitMultipart("POST", uri, params, headers, files) { f }

  def put[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    put(uri, params)(f)
  def put[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    put(uri, params, Map[String, String]())(f)
  def put[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    put(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def put[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A) =
    submit("PUT", uri, Seq.empty, headers, body) { f }
  def put[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, File)])(f: => A): A =
    put(uri, params, files, Map[String, String]()) { f }
  def put[A](uri: String, params: Iterable[(String, String)], files: Iterable[(String, File)], headers: Map[String, String])(f: => A): A =
    submitMultipart("PUT", uri, params, headers, files) { f }

  def delete[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("DELETE", uri, params, headers) { f }

  def options[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("OPTIONS", uri, params, headers) { f }

  def trace[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("TRACE", uri, params, headers) { f }

  def connect[A](uri: String, params: Iterable[(String, String)] = Seq.empty, headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("CONNECT", uri, params, headers) { f }

  def patch[A](uri: String, params: Tuple2[String, String]*)(f: => A): A =
    patch(uri, params)(f)
  def patch[A](uri: String, params: Iterable[(String,String)])(f: => A): A =
    patch(uri, params, Map[String, String]())(f)
  def patch[A](uri: String, params: Iterable[(String,String)], headers: Map[String, String])(f: => A): A =
    patch(uri, toQueryString(params), Map("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8") ++ headers)(f)
  def patch[A](uri: String, body: String = "", headers: Map[String, String] = Map.empty)(f: => A): A =
    submit("PATCH", uri, Seq.empty, headers, body) { f }

  private[test] def toQueryString(params: Traversable[(String, String)]) =
    params.map(t => List(t._1, t._2).map(encode(_, "UTF-8")).mkString("=")).mkString("&")

}
