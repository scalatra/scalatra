package org.scalatra

import org.atmosphere.cpr.AtmosphereResource
import java.util.UUID

package object atmosphere {
  type AtmoReceive = PartialFunction[InboundMessage, Unit]

  type ClientFilter = AtmosphereResource => Boolean

//  class SkipSelf(uuid: String) extends ClientFilter {
//    def apply(v1: AtmosphereResource): Boolean = v1.uuid() != uuid
//  }
//
//  class OnlySelf(uuid: String) extends ClientFilter {
//    def apply(v1: AtmosphereResource): Boolean = v1.uuid() == uuid
//  }
}
