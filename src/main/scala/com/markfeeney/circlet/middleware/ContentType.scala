package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.CpsConverters._
import com.markfeeney.circlet.{CpsMiddleware, Util, Middleware}
import com.markfeeney.circlet.Util.MimeTypes

/**
 * Middleware for automatically adding a content type to response objects based on the request URI.
 * e.g. request http://example.com/foo.mp4, adds Content-Type: video/mp4 to response.
 *
 * Port of https://github.com/ring-clojure/ring/blob/01de0cf1bbab402905bc65789bebb9a7dc36d974/ring-core/src/ring/middleware/content_type.clj
 */
object ContentType {

  def wrap(overrides: MimeTypes = Map.empty): Middleware = handler => req => {
    handler(req).map { resp =>
      if (resp.headers.get("Content-Type").isDefined) {
        resp
      } else {
        val mimeType = Util.mimeType(req.uri, overrides).getOrElse("application/octet-stream")
        resp.addHeader("Content-Type", mimeType)
      }
    }
  }

  final def wrapCps(overrides: MimeTypes = Map.empty): CpsMiddleware = wrap(overrides)

}
