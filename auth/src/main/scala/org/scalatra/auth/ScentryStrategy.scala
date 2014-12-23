package org.scalatra
package auth

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

trait ScentryStrategy[UserType <: AnyRef] {

  protected def app: ScalatraBase

  def name: String = "NameMe"

  def registerWith(registrar: Scentry[UserType]) {
    if (name == "NameMe") throwOverrideException
    else registrar.register(name, createStrategy _)
  }

  def createStrategy(app: ScalatraBase): this.type = {
    throwOverrideException
  }

  private def throwOverrideException = {
    throw new RuntimeException("This method is used when configuring strategies through web.xml.\n" +
      "If you want to use this registration method you have to override createStrategy and name in your strategy.\n" +
      "Your strategy also needs to have a parameterless constructor for it to be used through web.xml")
  }

  /**
   * Indicates if this strategy should be run.
   *
   * @return a Boolean to indicate validity
   */
  def isValid(implicit request: HttpServletRequest) = true

  /**
   * Perform the authentication for this strategy
   *
   * @return a UserType option where None indicates auth failure
   */
  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[UserType]

  /**
   * Perform stuff before authenticating, only run when the module is valid
   */
  def beforeAuthenticate(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff after authentication only run when the module is valid
   */
  def afterAuthenticate(winningStrategy: String, user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff before setting the user in the session
   */
  def beforeSetUser(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff after setting the user in the session
   */
  def afterSetUser(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff before fetching and serializing the user from session
   */
  def beforeFetch[IdType](userId: IdType)(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff after fetching and serializing the user from session
   */
  def afterFetch(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff before logging the user out and invalidating the session
   */
  def beforeLogout(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff after logging the user out and invalidating the session
   */
  def afterLogout(user: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) {}

  /**
   * Perform stuff when the request is unauthenticated and the strategy is valid
   */
  def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) {}

}
