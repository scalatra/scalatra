package org.scalatra

import util.MultiMapHeadView

import java.io.{InputStream}
import java.net.URI
import collection.{Map, mutable}

/**
 * A representation of an HTTP request.  Heavily influenced by the Rack
 * specification.
 */
trait HttpRequest extends HttpMessage with mutable.Map[String, AnyRef] {
  /**
   * The HTTP request method, such as GET or POST
   */
  def requestMethod: HttpMethod

  /**
   * The initial portion of the request URL's "path" that corresponds to
   * the application object, so that the application knows its virtual
   * "location". This may be an empty string, if the application corresponds
   * to the "root" of the server.
   */
  def scriptName: String

  /**
   * The remainder of the request URL's "path", designating the virtual
   * "location" of the request's target within the application. This may be
   * an empty string, if the request URL targets the application root and
   * does not have a trailing slash.
   */
  def pathInfo: String

  /**
   * The portion of the request URL that follows the ?, if any. May be
   * empty, but is always required!
   */
  def queryString: String

  /**
   * When combined with scriptName, pathInfo, and serverPort, can be used to
   * complete the URL.  Note, however, that the "Host" header, if present,
   * should be used in preference to serverName for reconstructing the request
   * URL.
   */
  def serverName: String

  /**
   * When combined with scriptName, pathInfo, and serverName, can be used to
   * complete the URL.  See serverName for more details.
   */
  def serverPort: Int

  /**
   * Http or Https, depending on the request URL.
   */
  def urlScheme: Scheme

  /**
   * Returns the length, in bytes, of the body, or None if not known.
   */
  def contentLength: Option[Long]

  /**
   * The input stream is an InputStream which contains the raw HTTP POST
   * data.  The caller should not close this stream.
   *
   * In contrast to Rack, this stream is not rewindable.  
   */
  def inputStream: InputStream


  def uri: URI

  def isSecure: Boolean

  /**
   * The version of the protocol the client used to send the request.
   * Typically this will be something like "HTTP/1.0"  or "HTTP/1.1" and may
   * be used by the application to determine how to treat any HTTP request
   * headers.
   */
  def serverProtocol: HttpVersion

  /**
   * A Map of the parameters of this request. Parameters are contained in
   * the query string or posted form data.
   */
  def multiParameters: MultiParams

  object parameters extends MultiMapHeadView[String, String] {
    protected def multiMap = multiParameters
  }

  def cookies: Map[String, String]

  /**
   * Caches and returns the body of the response.  The method is idempotent
   * for any given request.  The result is cached in memory regardless of size,
   * so be careful.  Calling this method consumes the request's input stream.
   *
   * @return the message body as a string according to the request's encoding
   * (defult ISO-8859-1).
   */
  def body: String

  /**
   * The remote address the client is connected from.
   * This takes the load balancing header X-Forwarded-For into account
   * @return the client ip address
   */
  def remoteAddress: String
}
