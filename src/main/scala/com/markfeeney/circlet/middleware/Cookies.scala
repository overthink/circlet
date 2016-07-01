package com.markfeeney.circlet.middleware

import java.util.Locale

import com.markfeeney.circlet.HttpParse.token
import com.markfeeney.circlet.{Middleware, Request, Util}
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

import scala.util.Try
import scala.util.matching.Regex

// from https://github.com/ring-clojure/ring/blob/91cf19b2a624cd4b8e282d23de78a56156054fc4/ring-core/src/ring/middleware/cookies.clj
object Cookies {

  // Represents Cookie request header (i.e. not relevant for Set-Cookie response header)
  type CookieMap = Map[String, String]

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

  private val rfc822Formatter = DateTimeFormat
    .forPattern("EEE, dd MMM yyyy HH:mm:ss Z")
    .withZone(DateTimeZone.UTC)
    .withLocale(Locale.US)

  // e.g. `Cookie: SID=31d4d96e407aad42; lang=en-US`
  private def parseCookieHeader(header: String, decoder: Decoder): CookieMap = {
    cookie
      .findAllMatchIn(header)
      .map(_.subgroups)
      .collect { case List(k, v) => (k, decoder(v)) }
      .collect { case (k, Some(v)) => (k, v) }
      .toMap
  }

  def get(req: Request): Option[CookieMap] = {
    Try(req.attrs("cookies").asInstanceOf[CookieMap]).toOption
  }

  def set(req: Request, cookies: CookieMap): Request = {
    req.updated("cookies", cookies)
  }

  def mw(
      decoder: Decoder = Util.formDecodeString,
      encoder: Encoder = Util.formEncodeString): Middleware = handler => req => k => {
    val req0 = req.getHeader("Cookie") match {
      case Some(v) => set(req, parseCookieHeader(v, decoder))
      case None => req
    }
    handler(req0)(k)
  }

}
