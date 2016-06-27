package com.markfeeney.circlet.middleware

import java.util.Locale

import com.markfeeney.circlet.HttpParse.token
import com.markfeeney.circlet.{Middleware, Request}
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

import scala.util.Try

// from https://github.com/ring-clojure/ring/blob/91cf19b2a624cd4b8e282d23de78a56156054fc4/ring-core/src/ring/middleware/cookies.clj
object Cookies {

  type CookieMap = Map[String, String]

  // RFC6265 cookie-octet
  private val cookieOctet = """[!#$%&'()*+\-./0-9:<=>?@A-Z\[\]\^_`a-z\{\|\}~]""".r

  // RFC6265 cookie-value
  // of note, can't get this to compile with string interpolation (?)
  private val cookieValue = ("\"" + cookieOctet + "*\"|" + cookieOctet + "*").r

  // FRC6265 set-cookie-string
  private val cookie = s"\\s*($token)=($cookieValue)\\s*[;,]?".r

  private val rfc822Formatter = DateTimeFormat
    .forPattern("EEE, dd MMM yyyy HH:mm:ss Z")
    .withZone(DateTimeZone.UTC)
    .withLocale(Locale.US)

  private def parseCookieHeader(header: String): CookieMap = {
    cookie
      .findAllMatchIn(header)
      .map(_.subgroups)
      .collect { case List(k, v) => (k, v) }
      .toMap
  }

  def get(req: Request): Option[CookieMap] = {
    Try(req.attrs("cookies").asInstanceOf[CookieMap]).toOption
  }

  def set(req: Request, cookies: CookieMap): Request = {
    req.updated("cookies", cookies)
  }

  def mw: Middleware = handler => req => k => {
    val req0 = set(req, parseCookieHeader(req.headers("cookie")))
    handler(req0)(k)
  }

}
