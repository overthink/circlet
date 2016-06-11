package com.markfeeney

package object circlet {
  type CpsHandler = (Request, Option[Response] => Sent.type ) => Sent.type
  type CpsMiddleware = CpsHandler => CpsHandler

  type Handler = Request => Option[Response]
  type Middleware = Handler => Handler
}
