package org.scalatra
package swagger

// separate files so the dependencies stay clean

import json.NativeJsonSupport

trait NativeSwaggerBase extends ScalatraSyntax with NativeJsonSupport with CorsSupport with SwaggerBase
