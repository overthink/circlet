package com.markfeeney.circlet.middleware

import java.util.Locale

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, Duration}

import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

/**
 * Represents a Cookie value to be used in the Set-Cookie response header.
 * https://tools.ietf.org/html/rfc6265
 *
 * @param value Value of the cookie
 * @param expires Date at which cookie expires
 * @param maxAge Interval of time (relative to now) after which the cookie expires
 * @param domain Domain on which cookie is valid
 * @param path Path under which the cookies is valid
 * @param secure Tell UA to only send cookie over secure channel
 * @param httpOnly Tell UA to prevent scripts from seeing cookie
 */
case class Cookie(
    value: String,
    expires: Option[DateTime] = None,  // expires must be sane-cookie-date; https://tools.ietf.org/html/rfc6265#section-4.1.1
    maxAge: Option[Duration] = None,
    domain: Option[String] = None,
    path: Option[String] = None,
    secure: Boolean = false,
    httpOnly: Boolean = false) {
}

object Cookie {
  implicit def str2Cookie(s: String): Cookie = Cookie(value = s)

  private val rfc822Formatter = DateTimeFormat
    .forPattern("EEE, dd MMM yyyy HH:mm:ss Z")
    .withZone(DateTimeZone.UTC)
    .withLocale(Locale.US)

  /**
   * Format `cookie` with given `name` for use in Set-Cookie header.
   */
  def toHeaderValue(name: String, cookie: Cookie): Option[String] = {
    if (name.trim.isEmpty) {
      None
    } else {
      val parts = new ListBuffer[String]
      parts.append(s"$name=${cookie.value}")
      cookie.expires.foreach { v => parts.append(s"Expires=${rfc822Formatter.print(v)}") }
      cookie.maxAge.foreach { v => parts.append(s"Max-Age=${v.getStandardSeconds}") }
      cookie.domain.foreach { v => parts.append(s"Domain=$v") }
      cookie.path.foreach { v => parts.append(s"Path=$v") }
      if (cookie.secure) { parts.append("Secure") }
      if (cookie.httpOnly) { parts.append("HttpOnly") }
      Some(parts.mkString("; "))
    }
  }
}
