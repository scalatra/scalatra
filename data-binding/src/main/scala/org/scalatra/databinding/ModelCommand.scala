package org.scalatra
package databinding

abstract class ModelCommand[T:Manifest] extends Command {
  type ModelType = T
}