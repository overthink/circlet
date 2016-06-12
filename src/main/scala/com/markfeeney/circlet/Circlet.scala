package com.markfeeney.circlet

object Circlet {

  /** Apply a middleware conditionally based on `pred`. */
  // via https://hackage.haskell.org/package/wai-3.2.1/docs/src/Network-Wai.html#ifRequest
  def ifRequest(pred: Request => Boolean): CpsMiddleware => CpsMiddleware = mw => handler => req => {
    if (pred(req)) {
      mw(handler)(req)
    } else {
      handler(req)
    }
  }

  /** Modify a response as a middleware. */
  // via https://hackage.haskell.org/package/wai-3.2.1/docs/src/Network-Wai.html#modifyResponse
  def modifyResponse(f: Response => Response): CpsMiddleware = handler => req => k => {
    handler(req) { optResp => k(optResp.map(f)) }
  }

}
