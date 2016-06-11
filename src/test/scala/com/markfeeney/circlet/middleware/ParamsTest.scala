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
    assert(ps.all == Map[String, Param]("foo" -> "bar"))
    assert(ps.queryParams == ps.all)
    assert(ps.formParams == Map.empty)
  }

  test("Seq and multi valued query string") {
    val ps = params("/test?x=hi+there&a=1&a=2&foo=bar&a=3")
    val expected = Map[String, Param](
      "a" -> Vector("1", "2", "3"),
      "foo" -> "bar",
      "x" -> "hi there"
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
    val expected = Map[String, Param](
      "foo" -> "bar",
      "a" -> Vector("1", "2 3")
    )
    assert(ps.formParams == expected)
    assert(ps.formParams == ps.all)
    assert(ps.queryParams == Map.empty)
  }

  test("query string params override form params") {
    val ps = params(formPost("/whatev?x=y&a=99", "foo=bar&a=1&a=2+3"))
    val expectedForm = Map[String, Param](
      "foo" -> "bar",
      "a" -> Vector("1", "2 3")
    )
    val expectedQuery = Map[String, Param]("x" -> "y", "a" -> "99")
    assert(ps.formParams == expectedForm)
    assert(ps.queryParams == expectedQuery)
    assert(ps.all == expectedForm ++ expectedQuery)
  }

  test("param values parsed from same area merge; from different area overwrite") {
    val ps = params(formPost("/whatev?x=1&x=2&y=1", "x=5&x=6&z=42"))
    val expectedAll = Map[String, Param](
      "x" -> Vector("1", "2"),
      "y" -> "1",
      "z" -> "42"
    )
    assert(ps.all == expectedAll)
  }

  test("query string param with no value") {
    val ps = params("/test?foo=bar&quux")
    assert(ps.all == Map[String, Param]("foo" -> "bar", "quux" -> Vector.empty))
    assert(ps.queryParams == ps.all)
    assert(ps.formParams == Map.empty)
  }

  test("weird query strings") {
    def t(ps: Params): Unit = {
      assert(ps.all == Map.empty)
      assert(ps.queryParams == ps.all)
      assert(ps.formParams == Map.empty)
    }
    withClue("no param name or value") {
      t(params("/test?&"))
    }
    withClue("empty string key, no value") {
      t(params("/test?&="))
      t(params("/test?=foo")) // key can't be empty string; ignored
    }
    withClue("mix of good and bizarre params") {
      val ps = params("/test?&=&foo&a=b")
      assert(ps.all == Map[String, Param]("foo" -> Vector.empty, "a" -> "b"))
      assert(ps.queryParams == ps.all)
      assert(ps.formParams == Map.empty)
    }
  }


}
