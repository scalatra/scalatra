package org.scalatra
package slf4j

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import org.scalatra.util.RicherString._

object LevelColorizer {
  private val EndColor = "\u001b[m"
  private val ErrorColor = "\u001b[0;31m"
  private val WarnColor = "\u001b[0;33m"
  private val InfoColor = "\u001b[0;32m"
  private val DebugColor = "\u001b[0;37m"

  private val colors = Map(
    Level.TRACE -> DebugColor,
    Level.DEBUG -> DebugColor,
    Level.INFO -> InfoColor,
    Level.WARN -> WarnColor,
    Level.ERROR -> ErrorColor)

}

/**
 * Adds ANSI colorization of the log level for use in the console.
 *
 * An example of a logback.xml file:
 *
 * {{{
 * <configuration>
 *    <conversionRule conversionWord="levelcolor"
 *                  converterClass="org.scalatra.slf4j.LevelColorizer" />
 *
 *    <!-- don't leak stuff coming from jul.Logging -->
 *    <contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator"/>
 *
 *    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *      <!-- encoders are assigned the type
 *           ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
 *      <encoder>
 *        <pattern>[%4levelcolor] [%d{ISO8601}] [%X{thread}] [%logger{4}]: %m%n</pattern>
 *      </encoder>
 *    </appender>
 *
 *    <root level="INFO">
 *      <appender-ref ref="STDOUT"/>
 *    </root>
 * </configuration>
 * }}}
 */
class LevelColorizer extends ClassicConverter {

  def convert(event: ILoggingEvent) = {
    import org.scalatra.slf4j.LevelColorizer._
    val c = colors.getOrElse(event.getLevel, "")
    "%s%s%s" format (c, event.getLevel, c.blankOption map (_ ⇒ EndColor) getOrElse "")
  }
}