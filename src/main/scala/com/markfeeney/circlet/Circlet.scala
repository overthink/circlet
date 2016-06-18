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

  /** Helper for building very simple handlers. */
  // e.g. val app = handler { req => Response(body = "Hello world") }
  def handler(f: Request => Response): CpsHandler = req => k => {
    k(Some(f(req)))
  }


  /** Useful to force execution of a handler in test. */
  // TODO: this is dumb, but Sent is package private so you can't test your handler without an actual
  // server adaptor which I don't want. For now this hack.  Later, maybe just open access to Sent?
  val mockSend: Cont = { _ => Sent }

}
