package com.markfeeney.circlet

/**
 * Basic response. Modeled after Response Map in https://github.com/ring-clojure/ring/blob/master/SPEC
 *
 * @param status The HTTP status code, must be greater than or equal to 100.
 * @param headers A map of HTTP header names to header values. These values may be
 *                either Strings, in which case one name/value header will be sent in the
 *                HTTP response, or a seq of Strings, in which case a name/value header will be
 *                sent for each such String value.
 * @param body A representation of the response body, if a response body is appropriate for the
 *             response's status code.
 * @param attrs Extra key/value data attached the the response.
 */
case class Response(
  status: Int = 200,
  headers: ResponseHeaders = Map.empty,
  body: Option[ResponseBody] = None,
  attrs: Map[String, AnyRef] = Map.empty) {

  /**
   * Helper for easily adding things to the attrs map.
   *
   * @param key Name of the thing to add
   * @param value The thing to add
   * @return A new HttpRequest with key/value added to attrs.
   */
  def updated(key: String, value: AnyRef): Response = {
    this.copy(attrs = attrs.updated(key, value))
  }

  /**
   * Add a response header to this Response.  Returns a new Response.
   */
  def addHeader(name: String, value: ResponseHeaderValue): Response = {
    this.copy(headers = this.headers.updated(name, value))
  }
}
