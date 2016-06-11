package com.markfeeney.circlet

import scala.language.implicitConversions

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
  headers: Map[String, Vector[String]] = Map.empty,
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
  def addHeader(name: String, value: Vector[String]): Response = {
    this.copy(headers = this.headers.updated(name, value))
  }

  def addHeader(name: String, value: String): Response = {
    addHeader(name, Vector(value))
  }
}

object Response {
  // This signficantly cleans up the syntax around building handlers with
  // -- I think -- little risk/surprise to users.
  implicit def resp2OptResp(resp: Response): Option[Response] = Some(resp)

  // This one I specifically do not want, and leave it commented out as a warning
  // to my future self.  It "works", but it's too surprising to magically
  // insert an Option#get() without the user being aware.
  // implicit def optResp2Resp(resp: Option[Response]): Response = resp.get
}
