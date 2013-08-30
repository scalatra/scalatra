package org.scalatra.atmosphere

/**
 * Useful filters to limit who receives broadcasts.
 */
trait AtmosphereClientFilter {

  /**
   * A unique identifier for a given connection. Can be used for filtering
   * purposes.
   */
  def uuid: String

  /**
   * Deliver the message to everyone except the current user.
   */
  final protected def SkipSelf: ClientFilter = _.uuid != uuid

  final protected def Others: ClientFilter = SkipSelf

  /**
   * Deliver the message only to the current user.
   */
  final protected def OnlySelf: ClientFilter = _.uuid == uuid

  final protected def Me: ClientFilter = OnlySelf

}

