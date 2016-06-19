package com.markfeeney

package object circlet {
  type Cont = Option[Response] => Sent.type
  type Handler = Request => Cont => Sent.type
  type Middleware = Handler => Handler
}
