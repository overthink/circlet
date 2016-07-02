package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.Circlet.handler
import com.markfeeney.circlet.TestUtils.complete
import com.markfeeney.circlet.middleware.Cookies.{Decoder, Encoder, RequestCookies, ResponseCookies}
import com.markfeeney.circlet.{Circlet, Request, Response, Util}
import org.joda.time.{DateTime, Period}
import org.scalatest.FunSuite

class CookiesTest extends FunSuite {

  private def getCookies(req: Request, decoder: Decoder = Util.formDecodeString): Option[RequestCookies] = {
    var cookies: Option[RequestCookies] = None
    val h = handler { req =>
      cookies = Cookies.get(req)
      Response()
    }
    Cookies.mw(decoder = decoder)(h)(req)(complete)
    cookies
  }

  private def t(cookieHeader: String, decoder: Decoder = Util.formDecodeString): RequestCookies = {
    val cookies = getCookies(Request.mock("/").addHeader("Cookie", cookieHeader), decoder)
    assert(cookies.isDefined, "Cookies expected in request")
    cookies.get
  }

  test("no cookie header") {
    val cookies = getCookies(Request.mock("/"))
    assert(cookies.isEmpty)
  }

  test("cooking parsing sanity") {
    assert(t("") == Map.empty)
    assert(t("foo=bar") == Map("foo" -> "bar"))
    assert(t("foo=bar; baz=42") == Map("foo" -> "bar", "baz" -> "42"))
    assert(t("  foo=bar;baz=42") == Map("foo" -> "bar", "baz" -> "42"))
    assert(t("  foo=bar;\tbaz=42") == Map("foo" -> "bar", "baz" -> "42"))
    assert(t(" ") == Map.empty)
    assert(t("=") == Map.empty)
    assert(t("x=") == Map("x" -> ""))
    assert(t("=x") == Map.empty)
  }

  test("cookies with whitespace after equals get empty value") {
    // correct? unsure really, but grammar forbids this
    assert(t("foo= bar;baz=42") == Map("foo" -> "", "baz" -> "42"))
  }

  test("cookies with whitespace before equals ignored") {
    // correct? unsure really, but grammar forbids this
    assert(t("foo =bar;baz=42") == Map("baz" -> "42"))
  }

  test("cookie values are urldecoded by default") {
    assert(t("a=hello+world") == Map("a" -> "hello world"))
    assert(t("a=hello%20world") == Map("a" -> "hello world"))
    assert(t("tz=America%2FToronto") == Map("tz" -> "America/Toronto"))
    assert(t("a=%e2%98%a0") == Map("a" -> "☠")) // Huh. Good job URLDecoder!
  }

  test("alternate cookie decoder works") {
    assert(t("a=hello+world", Some.apply) == Map("a" -> "hello+world"))
    assert(t("a=hello%20world", Some.apply) == Map("a" -> "hello%20world"))
    assert(t("a=%e2%98%a0", Some.apply) == Map("a" -> "%e2%98%a0"))
  }

  test("quoted cookies are untouched") {
    // Ring has special handling for double-quoted cookie values and I don't
    // know the full motivation.  Until I learn, quotes aren't touched.
    assert(t("a=\"hello+world\"") == Map("a" -> "\"hello world\""))
    assert(t("a=\"hello+world\"", Some.apply) == Map("a" -> "\"hello+world\""))
  }

  test("cookies with invalid url encoding are ignored") {
    assert(t("a=%z") == Map.empty)
    assert(t("a=%z;b=9000") == Map("b" -> "9000"))
  }

  private def getSetCookiesHeaders(
      cookies: ResponseCookies,
      encoder: Encoder): Option[Vector[String]] = {
    val h = handler(Cookies.set(Response(), cookies))
    val resp = Circlet.extractResponse(Cookies.mw(encoder = encoder)(h)(Request.mock("/"))).get
    resp.headers.get("Set-Cookie")
  }

  private def t2(
      cookies: ResponseCookies,
      encoder: Encoder = Util.formEncodeString): Option[Vector[String]] = {
    getSetCookiesHeaders(cookies, encoder)
  }

  test("no cookies set on response, no Set-Cookie") {
    assert(t2(Map.empty).isEmpty)
  }

  test("empty cookie name, no Set-Cookie") {
    assert(t2(Map("" -> "foo", " " -> "bar")).isEmpty)
  }

  test("can set simple cookies") {
    assert(t2(Map("foo" -> "bar")).get == Vector("foo=bar"))
    assert(t2(Map("foo" -> "bar", "baz" -> "42")).get == Vector("foo=bar", "baz=42"))
  }

  test("set cookies with expiry") {
    val dt = new DateTime("2100-01-01T00:00:00Z")
    val c = Cookie(value = "bar", expires = Some(dt))
    assert(t2(Map("foo" -> c)).get == Vector("foo=bar; Expires=Fri, 01 Jan 2100 00:00:00 +0000"))
  }

  test("set cookie with all attrs (even if nonsense)") {
    val dt = new DateTime("2100-01-01T00:00:00Z")
    val c = Cookie(
      value = "bar",
      expires = Some(dt),
      maxAge = Some(Period.hours(4).toStandardDuration),
      domain = Some("example.com"),
      path = Some("/foo/bar"),
      secure = true,
      httpOnly = true)
    val expected = Vector("foo=bar; Expires=Fri, 01 Jan 2100 00:00:00 +0000; Max-Age=14400; Domain=example.com; Path=/foo/bar; Secure; HttpOnly")
    assert(t2(Map("foo" -> c)).get == expected)
  }

  test("set cookies are urlencoded by default") {
    val actual = t2(Map("a" -> "hello world", "b" -> "foo/bar")).get
    assert(actual == Vector("a=hello+world", "b=foo%2Fbar"))
  }

  test("alternate cookie value encoder works") {
    val actual = t2(Map("a" -> "hello world", "b" -> "foo/bar"), Some.apply).get
    assert(actual == Vector("a=hello world", "b=foo/bar"))
  }

}
