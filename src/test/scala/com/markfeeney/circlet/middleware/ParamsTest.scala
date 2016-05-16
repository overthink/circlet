package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.{Handler, HttpMethod, Response, TestUtils}
import org.scalatest.FunSuite

class ParamsTest extends FunSuite {

  // helper to rip Params out of Request that the app ultimately sees
  private def params(url: String): Params = {
    var ps: Params = null
    val h: Handler = req => {
      ps = Params.get(req)
      Response()
    }
    val app = Params.wrap()(h)
    app(TestUtils.request(HttpMethod.Get, url))
    ps
  }

  test("query params") {

    withClue("typical query string") {
      val ps = params("/test?foo=bar&baz=quux")
      assert(ps.all == Map("foo" -> "bar", "baz" -> "quux"))
      assert(ps.queryParams == ps.all)
      assert(ps.formParams == Map.empty)
    }

    assert(params("/test?a=1&a=2&a=3") == Map("a" -> "????"), "Ack, what do I want to do here!")
  }

}
