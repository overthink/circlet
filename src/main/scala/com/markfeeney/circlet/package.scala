package com.markfeeney

package object circlet {
  type Cont = Option[Response] => Sent.type
  type CpsHandler = Request => Cont => Sent.type
  type CpsMiddleware = CpsHandler => CpsHandler

  type Handler = Request => Option[Response]
  type Middleware = Handler => Handler
}
