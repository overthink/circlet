package com.markfeeney.circlet

import org.scalatest.FunSuite
import com.markfeeney.circlet.Circlet.{handler, ifRequest}
import com.markfeeney.circlet.ResponseBody.StringBody

class CircletTest extends FunSuite {

  test("ifRequest sanity") {
    val mw: Middleware = _ => handler(Response(body = "body from mw"))
    val h = handler(Response(body = "body from h"))
    val h0: Handler = ifRequest(_.requestMethod == HttpMethod.Get)(mw)(h)

    h0(Request.mock(HttpMethod.Get, "/foo")) {
      case Some(resp) =>
        assert(resp.body.contains(StringBody("body from mw")), "mw should be applied to GET requests")
        Sent
      case None =>
        fail("expected response")
    }

    h0(Request.mock(HttpMethod.Post, "/foo")) {
      case Some(resp) =>
        assert(resp.body.contains(StringBody("body from h")), "mw should not be applied to POST requests")
        Sent
      case None =>
        fail("expected response")
    }

  }

}
