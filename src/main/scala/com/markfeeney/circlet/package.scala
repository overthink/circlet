package com.markfeeney

package object circlet {
  // A handler can opt to do nothing, hence the optional response.
  type Handler = HttpRequest => Option[HttpResponse]
  type ResponseHeaders = Map[String, ResponseHeaderValue]
}
