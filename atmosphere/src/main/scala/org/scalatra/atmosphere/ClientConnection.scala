package org.scalatra
package atmosphere

import java.util.UUID

trait ClientConnection {


  def uuid: UUID

  def receive: AtmoReceive


}