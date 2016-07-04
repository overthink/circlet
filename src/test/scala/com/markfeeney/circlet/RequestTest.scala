package com.markfeeney.circlet

import java.io.ByteArrayInputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.charset.StandardCharsets.{UTF_16, UTF_8}

import org.scalatest.FunSuite

class RequestTest extends FunSuite {

  test("charset parsing") {
    def parse(contentType: String): Option[Charset] = {
      val req = TestUtils.request(HttpMethod.Post, "http://example.com/whatev")
        .addHeader("content-type", contentType)
      req.characterEncoding
    }

    assert(parse("text/html; charset=utf-8").contains(UTF_8))
    assert(parse("text/html; charset = utf-8").isEmpty, "Spaces around = not allowed")
    assert(parse("text/html;   charset=uTF-16").contains(UTF_16))
    assert(parse("").isEmpty)
    assert(parse("bogus").isEmpty)
  }

  test("bodyString") {
    val s = "☠☠☠ This is a test! ☠☠☠"
    val is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))
    val req = Request.mock("/").copy(body = Some(is))
    assert(req.bodyString().contains(s))
  }

  test("bodyString, charset override works") {
    val s = "☠☠☠ This is a test! ☠☠☠"
    def t: Request = {
      val is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_16))
      Request.mock("/").copy(body = Some(is))
    }
    assert(!t.bodyString().contains(s))
    assert(t.bodyString(StandardCharsets.UTF_16).contains(s))
  }

}
