package org.scalatra.auth

trait ScentryStrategy[UserType <: AnyRef] {

  /**
   * Indicates if this strategy should be run.
   *
   * @return a Boolean to indicate validity
   */
  def valid_? = true

  /**
   * Perform the authentication for this strategy
   *
   * @return a UserType option where None indicates auth failure
   */
  def authenticate_! : Option[UserType]

  /**
   * Perform stuff before authenticating, only run when the module is valid
   */
  def beforeAuthenticate {}

  /**
   * Perform stuff after authentication only run when the module is valid
   */
  def afterAuthenticate {}

  /**
   * Perform stuff before setting the user in the session
   */
  def beforeSetUser {}

  /**
   * Perform stuff after setting the user in the session
   */
  def afterSetUser {}

  /**
   * Perform stuff before fetching and serializing the user from session
   */
  def beforeFetch {}

  /**
   * Perform stuff after fetching and serializing the user from session
   */
  def afterFetch {}

  /**
   * Perform stuff before logging the user out and invalidating the session
   */
  def beforeLogout {}

  /**
   * Perform stuff after logging hte user out and invalidating the session
   */
  def afterLogout {}

}