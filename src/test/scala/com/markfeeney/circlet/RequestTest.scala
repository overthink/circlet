package com.markfeeney.circlet

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.{UTF_8, UTF_16}
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

}
