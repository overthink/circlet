package com.markfeeney

package object circlet {
  type ResponseHeaders = Map[String, ResponseHeaderValue]

  type CpsHandler = (Request, Response => Done.type) => Done.type
  type CpsMiddleware = CpsHandler => CpsHandler

  type Handler = Request => Response
  type Middleware = Handler => Handler
}
