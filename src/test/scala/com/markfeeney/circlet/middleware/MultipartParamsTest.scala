package com.markfeeney.circlet.middleware

import java.nio.charset.StandardCharsets
import com.markfeeney.circlet.{Handler, HttpMethod, Request, Response, TestUtils, Util}
import org.scalatest.FunSuite

class MultipartParamsTest extends FunSuite {

  // helper to rip Params out of Request that the app ultimately sees
  private def params(req: Request): Params = {
    var ps: Params = null
    val h: Handler = req => {
      ps = Params.get(req)
      Response()
    }
    val app = MultipartParams.wrap()(h)
    app(req)
    ps
  }

  test("simple basic multipart") {

    val body = "--XXXX\r\n" +
      "Content-Disposition: form-data; name=\"upload\"; filename=\"test.txt\"\r\n" +
      "Content-Type: text/plain\r\n\r\n" +
      "foo bar!\r\n" +
      "--XXXX\r\n" +
      "Content-Disposition: form-data; name=\"baz\"\r\n\r\n" +
      "quux\r\n" +
      "--XXXX--"

    val req = TestUtils.request(HttpMethod.Get, "/test")
      .copy(body = Some(Util.stringInputStream(body)))
      .setContentType("multipart/form-data; boundary=XXXX")
      .setContentLength(body.getBytes(StandardCharsets.UTF_8).length)

    val ps = params(req)
    assert(ps.all == Map[String, Param]("foo" -> "bar"))
    assert(ps.queryParams == ps.all)
    assert(ps.formParams == Map.empty)
  }

}
