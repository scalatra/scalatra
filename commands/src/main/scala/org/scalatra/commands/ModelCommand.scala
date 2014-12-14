package org.scalatra
package commands

@deprecated("This was meant for usage with the `org.scalatra.commands.CommandHandler`, but that approach is not fully compiler verified. Look at using the `execute` method on a command instead.", "2.2.1")
abstract class ModelCommand[T: Manifest] extends Command {
  type ModelType = T
}