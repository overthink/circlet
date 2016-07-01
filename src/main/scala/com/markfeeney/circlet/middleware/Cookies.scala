package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.HttpParse.token
import com.markfeeney.circlet._

import scala.util.Try
import scala.util.matching.Regex

// from https://github.com/ring-clojure/ring/blob/91cf19b2a624cd4b8e282d23de78a56156054fc4/ring-core/src/ring/middleware/cookies.clj
object Cookies {

  /**
   * Request cookies come from Cookie request header, and are just string
   * values.
   */
  type RequestCookies = Map[String, String]

  /**
   * Response cookies end up as Set-Cookie response headers, and can have
   * various attributes aside from just a string value.
   */
  type ResponseCookies = Map[String, Cookie]

  /** For decoding cookie strings from the HTTP header (after UTF-8 decoding). */
  type Decoder = String => Option[String]

  /** For encoding values before they hit the HTTP header. */
  type Encoder = String => Option[String]

  // RFC6265 cookie-octet
  private val cookieOctet: String  = """[!#$%&'()*+\-./0-9:<=>?@A-Z\[\]\^_`a-z\{\|\}~]"""

  // RFC6265 cookie-value
  // of note, can't get this to compile with string interpolation (?)
  private val cookieValue: String  = "\"" + cookieOctet + "*\"|" + cookieOctet + "*"

  // FRC6265 set-cookie-string
  private val cookie: Regex = s"\\s*($token)=($cookieValue)\\s*[;,]?".r

  // e.g. `Cookie: SID=31d4d96e407aad42; lang=en-US`
  private def parseCookieHeader(header: String, decoder: Decoder): RequestCookies = {
    cookie
      .findAllMatchIn(header)
      .map(_.subgroups)
      .collect { case List(k, v) => (k, decoder(v)) }
      .collect { case (k, Some(v)) => (k, v) }
      .toMap
  }

  def get(req: Request): Option[RequestCookies] = {
    Try(req.attrs("cookies").asInstanceOf[RequestCookies]).toOption
  }

  def set(req: Request, cookies: RequestCookies): Request = {
    req.updated("cookies", cookies)
  }

  def get(resp: Response): Option[ResponseCookies] = {
    Try(resp.attrs("cookies").asInstanceOf[ResponseCookies]).toOption
  }

  def set(resp: Response, cookies: ResponseCookies): Response = {
    resp.updated("cookies", cookies)
  }

  /** Helper to add a cookie to a response. */
  def add(resp: Response, name: String, value: Cookie): Response = {
    set(resp, get(resp).getOrElse(Map()).updated(name, value))
  }

  // make Set-Cookie headers for each Cookie found on Response
  private def addSetCookieHeaders(resp: Response): Response = {
    get(resp) match {
      case None => resp
      case Some(respCookies) =>
        respCookies.foldLeft(resp) { case (acc, (k, v)) =>
          Cookie.toHeaderValue(k, v) match {
            case None => acc
            case Some(x) => acc.addHeader("Set-Cookie", x)
          }
        }
    }
  }

  def mw(
      decoder: Decoder = Util.formDecodeString,
      encoder: Encoder = Util.formEncodeString): Middleware = handler => req => k => {
    val req0 = req.getHeader("Cookie") match {
      case Some(v) => set(req, parseCookieHeader(v, decoder))
      case None => req
    }
    Circlet.modifyResponse(addSetCookieHeaders)(handler)(req0)(k)
  }

}

