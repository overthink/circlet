package com.markfeeney

package object circlet {
  type CpsHandler = (Request, Response => Sent.type ) => Sent.type
  type CpsMiddleware = CpsHandler => CpsHandler

  type Handler = Request => Response
  type Middleware = Handler => Handler
}
