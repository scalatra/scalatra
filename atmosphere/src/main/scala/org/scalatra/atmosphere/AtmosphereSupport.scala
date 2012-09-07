package org.scalatra
package atmosphere

import json.JsonSupport
import org.atmosphere.cpr.{AtmosphereResource, AtmosphereResourceEvent, AtmosphereHandler}

trait AtmosphereSupport { self: ScalatraBase with JsonSupport[_] =>

}