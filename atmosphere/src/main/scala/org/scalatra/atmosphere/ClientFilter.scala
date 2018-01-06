package org.scalatra.atmosphere

import org.atmosphere.cpr.AtmosphereResource

abstract class ClientFilter(val uuid: String) extends Function[AtmosphereResource, Boolean]

class Everyone extends ClientFilter(null) {
  def apply(v1: AtmosphereResource): Boolean = true
  override def toString(): String = "Everyone"
}

class OnlySelf(uuid: String) extends ClientFilter(uuid) {
  def apply(v1: AtmosphereResource): Boolean = v1.uuid == uuid
  override def toString(): String = "OnlySelf"
}

class SkipSelf(uuid: String) extends ClientFilter(uuid) {
  def apply(v1: AtmosphereResource): Boolean = v1.uuid != uuid
  override def toString(): String = "Others"
}
