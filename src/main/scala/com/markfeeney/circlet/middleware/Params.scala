package com.markfeeney.circlet.middleware

import java.nio.charset.{StandardCharsets, Charset}
import com.markfeeney.circlet.{CpsMiddleware, Middleware, Request}
import com.markfeeney.circlet.CpsConverters.middleware2Cps

object Params {

  /**
   * Adds params from the query string and request body to the `req`'s `attrs`
   * map under the key "params".  Returns new Request.
   */
  def request(req: Request, encoding: String = "UTF-8"): Request = {
    ???
  }

  def wrap(encoding: Charset = StandardCharsets.UTF_8): Middleware = handler => req => {
    handler(req)
  }

  def wrapCps(encoding: Charset): CpsMiddleware = wrap(encoding)
}
