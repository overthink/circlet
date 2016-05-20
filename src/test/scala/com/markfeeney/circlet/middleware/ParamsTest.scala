package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.{Util, Request, Handler, HttpMethod, Response, TestUtils}
import org.scalatest.FunSuite

class ParamsTest extends FunSuite {

  // helper to rip Params out of Request that the app ultimately sees
  private def params(req: Request): Params = {
    var ps: Params = null
    val h: Handler = req => {
      ps = Params.get(req)
      Response()
    }
    val app = Params.wrap()(h)
    app(req)
    ps
  }

  private def params(url: String): Params = {
    params(TestUtils.request(HttpMethod.Get, url))
  }

  test("simple query string") {
    val ps = params("/test?foo=bar")
    assert(ps.all == Map("foo" -> Seq("bar")))
    assert(ps.queryParams == ps.all)
    assert(ps.formParams == Map.empty)
  }

  test("Seq and multi valued query string") {
    val ps = params("/test?x=hi+there&a=1&a=2&foo=bar&a=3")
    val expected = Map(
      "a" -> Seq("1", "2", "3"),
      "foo" -> Seq("bar"),
      "x" -> Seq("hi there")
    )
    assert(ps.queryParams == expected)
    assert(ps.queryParams == ps.all)
    assert(ps.formParams == Map.empty)
  }

  private def formPost(path: String, body: String): Request = {
    TestUtils.request(HttpMethod.Post, path)
      .addHeader("content-type", "application/x-www-form-urlencoded")
      .copy(body = Some(Util.stringInputStream(body)))
  }

  test("form params") {
    val ps = params(formPost("/whatev", "foo=bar&a=1&a=2+3"))
    val expected = Map(
      "foo" -> Seq("bar"),
      "a" -> Seq("1", "2 3")
    )
    assert(ps.formParams == expected)
    assert(ps.formParams == ps.all)
    assert(ps.queryParams == Map.empty)
  }

  test("both form and query params") {
    val ps = params(formPost("/whatev?x=y&a=99", "foo=bar&a=1&a=2+3"))
    val expectedForm = Map(
      "foo" -> Seq("bar"),
      "a" -> Seq("1", "2 3")
    )
    val expectedQuery = Map("x" -> Seq("y"), "a" -> Seq("99"))
    assert(ps.formParams == expectedForm)
    assert(ps.queryParams == expectedQuery)
    assert(ps.all == expectedForm ++ expectedQuery, "query string params override form params")
  }

}
