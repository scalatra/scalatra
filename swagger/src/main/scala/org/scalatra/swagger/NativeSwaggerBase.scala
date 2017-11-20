package org.scalatra
package swagger

// separate files so the dependencies stay clean

import org.scalatra.json.NativeJsonSupport

trait NativeSwaggerBase extends ScalatraBase with NativeJsonSupport with CorsSupport with SwaggerBase
