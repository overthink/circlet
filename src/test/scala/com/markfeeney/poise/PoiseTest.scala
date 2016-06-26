package com.markfeeney.poise

import com.markfeeney.circlet._
import com.markfeeney.circlet.{HttpMethod => HM}
import com.markfeeney.circlet.Circlet.{extractResponse, handler}
import com.markfeeney.poise.Poise.{Simple, ifRoute}
import com.markfeeney.circlet.ResponseBody.StringBody
import org.scalatest.FunSuite

class PoiseTest extends FunSuite {

  private def assertResp(f: Response => Unit): Cont = {
    case Some(resp) => f(resp); Sent
    case None => fail("expected response")
  }

  private def assertNoResp: Cont = {
    case Some(resp) => fail(s"did not expect response, got $resp")
    case None => assert(true, "expected non-match"); Sent
  }

  test("ifRoute sanity") {
    val r1 = ifRoute("/foo/:id")(handler(Response(body = "Foo")))

    r1(Request.mock("/foo/42"))(assertResp { resp =>
      assert(resp.status == 200)
      assert(resp.body.contains(StringBody("Foo")))
    })

    r1(Request.mock("/bar/9000"))(assertNoResp)
  }

  test("multiple route choices") {
    def h(path: String, body: String) = ifRoute(path) { handler(Response(body = body)) }

    val r1: Handler = h("/foo/:id", "hi1")
    val r2: Handler = h("/bar/:id", "hi2")
    val r3: Handler = h("/heynow", "hi3")
    def notFound(reason: String) = handler(Response(status = 404, body = reason))

    val composed: Handler = Poise.or(r1, r2, r3, notFound("Computer said no."))

    def t(url: String): String = {
      extractResponse(composed(Request.mock(url))) match {
        case Some(Response(_, _, Some(StringBody(s)), _)) => s
        case other => fail(s"unexpected response: $other")
      }
    }

    assert(t("/foo/42") == "hi1")
    assert(t("/bar/asdf") == "hi2")
    assert(t("/heynow") == "hi3")
    assert(t("/badurl") == "Computer said no.")
    assert(t("/foo/9000/badurl") == "Computer said no.")
  }

  test("typical route matching") {
    val h: Handler = Simple.GET("/foo/:id") { req =>
      val body: String = Route.get(req).get.params("id").value
      Response(body = s"id was $body")
    }

    h(Request.mock(HM.Get, "/foo/42"))(assertResp { resp =>
      assert(resp.body.contains(StringBody("id was 42")))
    })

    h(Request.mock(HM.Post, "/foo/42"))(assertNoResp)
  }

  ignore("building routes with context") {

  }

}
