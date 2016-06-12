package com.markfeeney.circlet

import com.markfeeney.circlet.middleware.Head
import org.scalatest.FunSuite

class HandlerTest extends FunSuite {
  test("simple") {
    val app: Handler = request => {
      Response(status = 200, body = "simple handler response")
    }

    val app0 = Head.wrap(app)

    val req = Request(
      uri = "/test",
      serverPort = 80,
      serverName = "foobar.com",
      remoteAddr = "localhost",
      requestMethod = HttpMethod.Head
    )

    val resp = app0(req).get
    assert(resp.status == 200)
    assert(resp.body.isEmpty)

  }

  test("fancy") {
    val app: CpsHandler = req => k => {
      val resp = Response(status = 200, body = "This is a body!")
      val ret = k(resp)
      ret
    }

    val app0: CpsHandler = Head.wrapCps(app)

    val req = Request(
      uri = "/test",
      serverPort = 80,
      serverName = "foobar.com",
      remoteAddr = "localhost",
      requestMethod = HttpMethod.Head
    )

    app0(req) {
      case None => fail("expected response")
      case Some(resp) =>
        assert(resp.status == 200)
        assert(resp.body.isEmpty)
        Sent
    }
  }
}
