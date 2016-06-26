package com.markfeeney.circlet

object Circlet {

  /** Apply a middleware conditionally based on `pred`. */
  // via https://hackage.haskell.org/package/wai-3.2.1/docs/src/Network-Wai.html#ifRequest
  def ifRequest(pred: Request => Boolean): Middleware => Middleware = mw => handler => req => {
    if (pred(req)) {
      mw(handler)(req)
    } else {
      handler(req)
    }
  }

  /** Modify a response as a middleware. */
  // via https://hackage.haskell.org/package/wai-3.2.1/docs/src/Network-Wai.html#modifyResponse
  def modifyResponse(f: Response => Response): Middleware = handler => req => k => {
    handler(req) { optResp => k(optResp.map(f)) }
  }

  /** Helper for building very simple handlers. */
  // e.g. val app = handler { req => Response(body = "Hello world") }
  def handler(f: Request => Option[Response]): Handler = req => k => k(f(req))

  /** Defines a handler that always returns `resp`. */
  def handler(resp: Response): Handler = handler { _ => resp }

  /**
   * Helper to rip response out of handler. Intended use: only in poorly designed tests,
   * emergencies.
   *
   * e.g. `val response = extractResponse(h(req)).get`
   */
  def extractResponse(f: Cont => Sent.type): Option[Response] = {
    var captured: Option[Response] = None
    f { resp =>
      captured = resp
      Sent
    }
    captured
  }

}
