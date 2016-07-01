package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.Circlet.handler
import com.markfeeney.circlet.TestUtils.complete
import com.markfeeney.circlet.middleware.Cookies.{CookieMap, Decoder}
import com.markfeeney.circlet.{Request, Response, Util}
import org.scalatest.FunSuite

class CookiesTest extends FunSuite {

  private def getCookies(req: Request, decoder: Decoder = Util.formDecodeString): Option[CookieMap] = {
    var cookies: Option[CookieMap] = None
    val h = handler { req =>
      cookies = Cookies.get(req)
      Response()
    }
    Cookies.mw(decoder = decoder)(h)(req)(complete)
    cookies
  }

  private def t(cookieHeader: String, decoder: Decoder = Util.formDecodeString): CookieMap = {
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

}
