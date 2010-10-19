package org.scalatra.util

trait MapWithIndifferentAccess[V] {
 def apply(key:String):V
 def get(key: Symbol): V = apply(key)
 def apply(key: Symbol): V = apply(key.name)
 //def apply(keys: Symbol*): Seq[V] = keys map get _
}


