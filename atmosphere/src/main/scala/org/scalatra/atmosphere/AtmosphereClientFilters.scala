package org.scalatra.atmosphere

/**
 * Useful filters to limit who receives broadcasts.
 */
trait AtmosphereClientFilters extends Serializable {

  /**
   * A unique identifier for a given connection. Can be used for filtering
   * purposes.
   */
  def uuid: String

  final protected def Everyone: ClientFilter = new Everyone

  /**
   * Deliver the message to everyone except the current user.
   */
  final protected def SkipSelf: ClientFilter = new SkipSelf(uuid)

  final protected def Others: ClientFilter = SkipSelf

  /**
   * Deliver the message only to the current user.
   */
  final protected def OnlySelf: ClientFilter = new OnlySelf(uuid)

  final protected def Me: ClientFilter = OnlySelf

}

