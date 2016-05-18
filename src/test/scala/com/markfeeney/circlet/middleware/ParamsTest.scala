package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.StrVal.{Multi, Single}
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

  test("simsple query string") {
    val ps = params("/test?foo=bar")
    assert(ps.all == Map("foo" -> Single("bar")))
    assert(ps.queryParams == ps.all)
    assert(ps.formParams == Map.empty)
  }

  test("single and multi valued query string") {
    val ps = params("/test?x=hi+there&a=1&a=2&foo=bar&a=3")
    val expected = Map(
      "a" -> Multi(Seq("1", "2", "3")),
      "foo" -> Single("bar"),
      "x" -> Single("hi there")
    )
    assert(ps.queryParams == expected)
    assert(ps.queryParams == ps.all)
    assert(ps.formParams == Map.empty)
  }

}
