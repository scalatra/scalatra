package org.scalatra
package auth

import servlet.ServletBase

import javax.servlet.http.{HttpSession, HttpServletRequest, Cookie}

trait ScentryStrategy[UserType <: AnyRef] {

  protected def app: ServletBase
  def name: Symbol = 'NameMe

  def registerWith(registrar: Scentry[UserType]) {
    if (name == 'NameMe) throwOverrideException
    else registrar.registerStrategy(name, createStrategy _)
  }


  def createStrategy(app: ServletBase): this.type = {
    throwOverrideException
  }

  private def throwOverrideException = {
    throw new RuntimeException("This method is used when configuring strategies through web.xml.\n" +
        "If you want to use this registration method you have to override createStrategy and name in your strategy.\n" +
        "Your strategy also needs to have a parameterless constructor for it to be used through web.xml"
      )
  }

  /**
   * Indicates if this strategy should be run.
   *
   * @return a Boolean to indicate validity
   */
  def isValid = true

  @deprecated("use isValid", "2.0.0")
  def valid_? = isValid

  /**
   * Perform the authentication for this strategy
   *
   * @return a UserType option where None indicates auth failure
   */
  def authenticate(): Option[UserType]

  @deprecated("use authenticate()", "2.0.0")
  def authenticate_! = authenticate()

  /**
   * Perform stuff before authenticating, only run when the module is valid
   */
  def beforeAuthenticate {}

  /**
   * Perform stuff after authentication only run when the module is valid
   */
  def afterAuthenticate(winningStrategy: Symbol, user: UserType) {}

  /**
   * Perform stuff before setting the user in the session
   */
  def beforeSetUser(user: UserType) {}

  /**
   * Perform stuff after setting the user in the session
   */
  def afterSetUser(user: UserType) {}

  /**
   * Perform stuff before fetching and serializing the user from session
   */
  def beforeFetch[IdType](userId: IdType) {}

  /**
   * Perform stuff after fetching and serializing the user from session
   */
  def afterFetch(user: UserType) {}

  /**
   * Perform stuff before logging the user out and invalidating the session
   */
  def beforeLogout(user: UserType) {}

  /**
   * Perform stuff after logging the user out and invalidating the session
   */
  def afterLogout(user: UserType) {}

  /**
   * Perform stuff when the request is unauthenticated and the strategy is valid
   */
  def unauthenticated() {}

}
