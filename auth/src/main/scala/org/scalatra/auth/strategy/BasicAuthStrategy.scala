package org.scalatra
package auth
package strategy

import java.util.Locale
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import net.iharder.Base64
import org.scalatra.util.RicherString._

import scala.io.Codec

trait RemoteAddress { self: ScentryStrategy[_] =>

  import org.scalatra.auth.strategy.BasicAuthStrategy._

  protected def remoteAddress(implicit request: HttpServletRequest) = {
    val proxied = request.getHeader("X-FORWARDED-FOR")
    val res = if (proxied.nonBlank) proxied else request.getRemoteAddr
    res
  }
}

/**
 * Provides a hook for the basic auth strategy
 *
 * for more details on usage check:
 * https://gist.github.com/732347
 */
trait BasicAuthSupport[UserType <: AnyRef] { self: (ScalatraBase with ScentrySupport[UserType]) =>

  def realm: String

  protected def basicAuth()(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    val baReq = new BasicAuthStrategy.BasicAuthRequest(request)
    if (!baReq.providesAuth) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"%s\"" format realm)
      halt(401, "Unauthenticated")
    }
    if (!baReq.isBasicAuth) {
      halt(400, "Bad Request")
    }
    scentry.authenticate("Basic")
  }

}

object BasicAuthStrategy {

  private val AUTHORIZATION_KEYS = List("Authorization", "HTTP_AUTHORIZATION", "X-HTTP_AUTHORIZATION", "X_HTTP_AUTHORIZATION")
  class BasicAuthRequest(r: HttpServletRequest) {

    def parts = authorizationKey map { r.getHeader(_).split(" ", 2).toList } getOrElse Nil
    def scheme: Option[String] = parts.headOption.map(sch => sch.toLowerCase(Locale.ENGLISH))
    def params = parts.lastOption

    private def authorizationKey = AUTHORIZATION_KEYS.find(r.getHeader(_) != null)

    def isBasicAuth = (false /: scheme) { (_, sch) => sch == "basic" }
    def providesAuth = authorizationKey.isDefined

    private[this] var _credentials: Option[(String, String)] = None
    def credentials = {
      if (_credentials.isEmpty)
        _credentials = params map { p =>
          (null.asInstanceOf[(String, String)] /: new String(Base64.decode(p), Codec.UTF8.charSet).split(":", 2)) { (t, l) =>
            if (t == null) (l, null) else (t._1, l)
          }
        }
      _credentials
    }
    def username = credentials map { _._1 } getOrElse null
    def password = credentials map { _._2 } getOrElse null
  }
}
abstract class BasicAuthStrategy[UserType <: AnyRef](protected val app: ScalatraBase, realm: String)
    extends ScentryStrategy[UserType]
    with RemoteAddress {

  private[this] val REMOTE_USER = "REMOTE_USER"

  implicit def request2BasicAuthRequest(r: HttpServletRequest) = new BasicAuthStrategy.BasicAuthRequest(r)

  protected def challenge = "Basic realm=\"%s\"" format realm

  override def isValid(implicit request: HttpServletRequest) = request.isBasicAuth && request.providesAuth

  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) =
    validate(request.username, request.password)

  protected def getUserId(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse): String
  protected def validate(userName: String, password: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[UserType]

  override def afterSetUser(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {
    response.setHeader(REMOTE_USER, getUserId(user))
  }

  override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) {
    app halt Unauthorized(headers = Map("WWW-Authenticate" -> challenge))
  }

  override def afterLogout(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {
    response.setHeader(REMOTE_USER, "")
  }
}
