package com.markfeeney.poise

import com.markfeeney.circlet._
import com.markfeeney.circlet.Circlet.{extractResponse, handler}
import com.markfeeney.poise.Poise.ifRoute
import com.markfeeney.circlet.ResponseBody.StringBody
import org.scalatest.FunSuite

class PoiseTest extends FunSuite {

  def get(url: String): Request = {
    Request(
      serverPort = 80,
      serverName = "example.com",
      remoteAddr = "localhost",
      uri = url,
      requestMethod = HttpMethod.Get
    )
  }

  test("ifRoute sanity") {
    val r1 = Poise.ifRoute(Route.compile("/foo/:id"))(handler(Response(body = "Foo")))
    r1(get("/foo/42")) {
      case None => fail("expected response")
      case Some(resp) =>
        assert(resp.status == 200)
        assert(resp.body.contains(StringBody("Foo")))
        Sent
    }
    r1(get("/bar/9000")) {
      case None =>
        assert(true, "route doesn't match url")
        Sent
      case Some(resp) =>
        fail(s"expected non-match, got $resp")
    }
  }

  test("multiple route choices") {
    def h(path: String, body: String) = ifRoute(Route.compile(path)) { handler(Response(body = body)) }

    val r1: Handler = h("/foo/:id", "hi1")
    val r2: Handler = h("/bar/:id", "hi2")
    val r3: Handler = h("/heynow", "hi3")
    def notFound(reason: String) = handler(Response(status = 404, body = reason))

    val composed: Handler = Poise.tryHandlers(r1, r2, r3, notFound("Computer said no."))

    def t(url: String): String = {
      extractResponse(composed(get(url))) match {
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


}
