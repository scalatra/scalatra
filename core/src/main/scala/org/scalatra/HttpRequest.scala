
package org.scalatra

import java.io.{InputStream}
import util.MultiMap
import java.net.URI
import collection.{GenSeq, Map, mutable}

/**
 * A representation of an HTTP request.
 */
trait HttpRequest extends HttpMessage with mutable.Map[String, AnyRef] {
  /**
   * The HTTP request method, such as GET or POST
   */
  def method: HttpMethod

  def uri: URI

  def isSecure: Boolean

  /**
   * The initial portion of the request URL's "path" that corresponds to the application object, so that the
   * application knows its virtual "location". This may be an empty string, if the application corresponds to
   * the "root" of the server.
   */
  def appPath: String

  /**
   * The remainder of the request URL's "path", designating the virtual "location" of the request's target within
   * the application. This may be an empty string, if the request URL targets the application root and does not have
   * a trailing slash.
   */
  def path: String

  /**
   * The portion of the request URL that follows the ?, if any. May be empty, but is always required!
   */
  // def queryString: MultiMap

  /**
   * The contents of any Content-Type fields in the HTTP request, or None if absent.
   */
  def contentType: Option[String]

  /**
   * The contents of any Content-Length fields in the HTTP request, or None if absent.
   */
  def contentLength: Option[Long]

  /**
   * When combined with scriptName, pathInfo, and serverPort, these variables can be used to complete the URL.
   * Note, however, that the "Host" header, if present, should be used in preference to serverName for reconstructing
   * the request URL.
   */
  def serverName: String

  /**
   * When combined with scriptName, pathInfo, and serverName, these variables can be used to complete the URL.
   * See serverName for more details.
   */
  def serverPort: Int

  /**
   * The version of the protocol the client used to send the request. Typically this will be something like "HTTP/1.0"
   * or "HTTP/1.1" and may be used by the application to determine how to treat any HTTP request headers.
   */
  def serverProtocol: HttpVersion

  /**
   * A map corresponding to the client-supplied HTTP request headers.
   *
   * TODO If the header is absent from the request, should get return Some(Seq.empty) or None?
   */
  def headers: Map[String, String]

  /**
   * A string representing the "scheme" portion of the URL at which the application is being invoked. Normally, this
   * will have the value "http" or "https", as appropriate.
   */
  def scheme: String

  /**
   * An input stream from which the HTTP request body can be read. (The server or gateway may perform reads on-demand
   * as requested by the application, or it may pre-read the client's request body and buffer it in-memory or on disk,
   * or use any other technique for providing such an input stream, according to its preference.)
   *
   * It is the responsibility of the caller to close the input stream.
   */
  def inputStream: InputStream

/*
  /**
   * A map in which the server or application may store its own data.
   */
  lazy val attributes: mutable.Map[String, Any] = mutable.Map.empty
*/

  /**
   * A Map of the parameters of this request. Parameters are contained in the query string or posted form data.
   */
  def parameters: ScalatraKernel.MultiParams

  // TODO def files: GenSeq[HttpFile]

  // TODO def cookies: CookieJar
}
