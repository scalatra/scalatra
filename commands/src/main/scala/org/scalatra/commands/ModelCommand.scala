package org.scalatra
package commands

abstract class ModelCommand[T:Manifest] extends Command {
  type ModelType = T
}