package com.markfeeney.circlet

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.{UTF_8, UTF_16}
import org.scalatest.FunSuite

class RequestTest extends FunSuite {

  test("charset parsing") {
    def getCharset(contentType: String): Option[Charset] = {
      val req = TestUtils.request(HttpMethod.Post, "http://example.com/whatev")
        .addHeader("content-type", contentType)
      req.characterEncoding
    }

    assert(getCharset("text/html; charset=utf-8").contains(UTF_8))
    assert(getCharset("text/html; charset = utf-8").isEmpty, "No spaces around =")
    assert(getCharset("text/html;   charset=uTF-16").contains(UTF_16))
    assert(getCharset("").isEmpty)
    assert(getCharset("bogus").isEmpty)
  }

}
