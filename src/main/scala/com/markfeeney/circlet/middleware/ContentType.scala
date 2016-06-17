package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.Util.MimeTypes
import com.markfeeney.circlet.{CpsMiddleware, Util}

/**
 * Middleware for automatically adding a content type to response objects based on the request URI.
 * e.g. request http://example.com/foo.mp4, adds Content-Type: video/mp4 to response.
 *
 * Port of https://github.com/ring-clojure/ring/blob/01de0cf1bbab402905bc65789bebb9a7dc36d974/ring-core/src/ring/middleware/content_type.clj
 */
object ContentType {

  def apply(overrides: MimeTypes = Map.empty): CpsMiddleware = handler => req => k => {
    handler(req) { resp =>
      val resp0 =
        for {
          r <- resp if r.contentType.isEmpty
          mimeType = Util.mimeType(req.uri, overrides).getOrElse("application/octet-stream")
        } yield r.setContentType(mimeType)
      k(resp0)
    }
  }

}
