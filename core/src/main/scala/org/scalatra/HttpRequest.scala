package org.scalatra

import util.{HeaderWithQParser, MultiMapHeadView}

import java.net.URI
import collection._
import generic.{Shrinkable, Growable}
import scala.Iterator
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._
import java.util.Locale
import java.io.InputStream
import com.google.common.collect.MapMaker

/**
 * A representation of an HTTP request.  Heavily influenced by the Rack
 * specification.
 */
trait HttpRequest extends HttpMessage with Growable[(String, Any)] with Shrinkable[String] {
  /**
   * The HTTP request method, such as GET or POST
   */
  def requestMethod: HttpMethod

  private[scalatra] def requestMethod_=(meth: HttpMethod)

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
   * A Map of the parameters of this request. Parameters are contained in
   * the query string or posted form data.
   */
  def multiParameters: MultiParams

  object parameters extends MultiMapHeadView[String, String] {
    protected def multiMap = multiParameters
  }

  def cookies: CookieJar

  def referrer: Option[String] = headers.get(HeaderNames.Referer).flatMap(_.blankOption)

  /**
   * Caches and returns the body of the response.  The method is idempotent
   * for any given request.  The result is cached in memory regardless of size,
   * so be careful.  Calling this method consumes the request's input stream.
   *
   * @return the message body as a string according to the request's encoding
   * (default ISO-8859-1).
   */
  def body: String

  def files: GenSeq[HttpFile]

  /**
   * The remote address the client is connected from.
   * This takes the load balancing header X-Forwarded-For into account
   * @return the client ip address
   */
  def remoteAddress: String

  /**
   * A map in which the server or application may store its own data.
   */
  protected lazy val attributes: mutable.ConcurrentMap[String, Any] = new MapMaker().makeMap[String, Any]().asScala
  def contains(key: String) = attributes.contains(key)
  def get(key: String) = attributes.get(key)
  def getOrElseUpdate(key: String, value: => Any) = get(key) match {
    case Some(v) => v
    case _ =>
      val v = value
      attributes(key) = v
      v
  }
  def apply(key: String) = attributes(key)
  def update(key: String, value: Any) = attributes(key) = value

  lazy val locales: Seq[Locale] = {
    val languages = new HeaderWithQParser(headers).parse(HeaderNames.AcceptLanguage)
    if (languages.isEmpty) {
      List(Locale.getDefault)
    } else {
      val r = languages filter (_.indexOf('-') > -1) map { lang =>
        val parts = lang.split('-')
        if (parts.size > 1) new Locale(parts(0).trim, parts(1).trim)
        else if (parts.size > 0) new Locale(parts(0).trim)
        else null
      }
      if (r.isEmpty) List(Locale.getDefault)
      else r
    }
  }

  lazy val locale = locales.head

  def +=(kv: (String, Any)) = {
    attributes += kv
    this
  }

  def -=(key: String) = {
    attributes -= key
    this
  }

  def iterator: Iterator[(String, Any)] = attributes.iterator
  def keysIterator: Iterator[String] = attributes.keysIterator

  def clear() { attributes.clear() }
}
