package org.scalatra
package auth
package strategy

import util.RicherString._

import org.scalatra.auth.{ScentrySupport, ScentryStrategy}
import net.iharder.Base64
import java.nio.charset.Charset
import java.util.Locale

trait RemoteAddress { self: ScentryStrategy[_]  =>

  protected def remoteAddress ={
    val proxied = app.request.headers("X-FORWARDED-FOR")
    val res = if (proxied.nonBlank) proxied else app.request.remoteAddress
    res
  }
}

/**
 * Provides a hook for the basic auth strategy
 *
 * for more details on usage check:
 * https://gist.github.com/732347
 */
trait BasicAuthSupport[UserType <: AnyRef] { self: (ScalatraApp with ScentrySupport[UserType])  =>

  def realm: String


  protected def basicAuth() = {
    val baReq = new BasicAuthStrategy.BasicAuthRequest(request)
    if(!baReq.providesAuth) {
      response.headers += "WWW-Authenticate" -> "Basic realm=\"%s\"".format(realm)
      halt(401, "Unauthenticated")
    }
    if(!baReq.isBasicAuth) {
      halt(400, "Bad Request")
    }
    scentry.authenticate('Basic)
  }

}

object BasicAuthStrategy {

  private val AUTHORIZATION_KEYS = List("Authorization", "HTTP_AUTHORIZATION", "X-HTTP_AUTHORIZATION", "X_HTTP_AUTHORIZATION")
  class BasicAuthRequest(r: HttpRequest) {

    def parts = authorizationKey flatMap { r.headers.get(_).map(_.split(" ", 2).toList) } getOrElse Nil
    def scheme: Option[Symbol] = parts.headOption.map(sch => Symbol(sch.toLowerCase(Locale.ENGLISH)))
    def params = parts.lastOption

    private def authorizationKey = AUTHORIZATION_KEYS find r.contains

    def isBasicAuth = (false /: scheme) { (_, sch) => sch == 'basic }
    def providesAuth = authorizationKey.isDefined

    private var _credentials: Option[(String, String)] = None
    def credentials = {
      if (_credentials.isEmpty )
        _credentials = params map { p =>
          (null.asInstanceOf[(String, String)] /: new String(Base64.decode(p), Charset.forName("UTF-8")).split(":", 2)) { (t, l) =>
            if(t == null) (l, null) else (t._1, l)
          }
        }
      _credentials
    }
    def username = credentials map { _._1 } getOrElse null
    def password = credentials map { _._2 } getOrElse null
  }

  private val REMOTE_USER = "REMOTE_USER"

}
abstract class BasicAuthStrategy[UserType <: AnyRef](protected val app: ScalatraApp, realm: String)
  extends ScentryStrategy[UserType]
  with RemoteAddress {

  import BasicAuthStrategy._


  implicit def request2BasicAuthRequest(r: HttpRequest) = new BasicAuthRequest(r)

  protected def challenge = "Basic realm=\"%s\"" format realm


  override def isValid = {
    app.request.isBasicAuth && app.request.providesAuth
  }

  def authenticate() = {
    val req = app.request
    validate(req.username, req.password)
  }

  protected def getUserId(user: UserType): String
  protected def validate(userName: String, password: String): Option[UserType]


  override def afterSetUser(user: UserType) {
    app.response.headers += REMOTE_USER -> getUserId(user)
  }


  override def unauthenticated() {
    app.response.headers += "WWW-Authenticate" -> challenge
    app.halt(401, "Unauthenticated")
  }

  override def afterLogout(user: UserType) {
    app.response.headers += REMOTE_USER -> ""
  }
}
