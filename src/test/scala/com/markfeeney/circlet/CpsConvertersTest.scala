package com.markfeeney.circlet

import com.markfeeney.circlet.HttpMethod.Get
import com.markfeeney.circlet.ResponseBody.StringBody
import org.scalatest.FunSuite
import com.markfeeney.circlet.CpsConverters._

class CpsConvertersTest extends FunSuite {

  test("Handler converted to CpsHandler still works") {

    def runAsserts(resp: Response): Unit = {
      assert(resp.body.contains(StringBody("Foo!")))
      assert(resp.headers == Map("X-Foo" -> Seq("42")))
    }

    val h: Handler = req => Response(body = "Foo!", headers = Map("X-Foo" -> Seq("42")))
    val req = TestUtils.request(HttpMethod.Get, "/")
    runAsserts(h(req))

    val cpsH: CpsHandler = h
    cpsH(req, resp => {
      withClue("Same asserts work on resp created with CPS handler") {
        runAsserts(resp)
      }
      Done
    })

  }

  test("Middleware converted to CpsMiddleware still works") {
    val mw: Middleware = handler => req => {
      val req0 = req.copy(headers = req.headers.updated("new-req-header", "true"))
      val resp = handler(req0)
      resp.copy(headers = resp.headers.updated("new-resp-header", Seq("true")))
    }

    val h: Handler = req => {
      // save the request received so we can inspect it for test purposes
      Response(body = "Hello world", attrs = Map("request" -> req))
    }

    withClue("test basic middleware") {
      val h0 = mw(h)
      val resp = h0(TestUtils.request(Get, "/"))
      val savedReq = resp.attrs("request").asInstanceOf[Request]
      assert(resp.body.contains(StringBody("Hello world")))
      assert(savedReq.headers.get("new-req-header").contains("true"))
      assert(resp.headers.get("new-resp-header").contains(Seq("true")))
    }

    withClue("same tests with CPS middleware") {
      val cpsH: CpsHandler = mw(h) // 2 layers of implicits enable this, sorry
      cpsH(TestUtils.request(Get, "/"), resp => {
        val savedReq = resp.attrs("request").asInstanceOf[Request]
        assert(resp.body.contains(StringBody("Hello world")))
        assert(savedReq.headers.get("new-req-header").contains("true"))
        assert(resp.headers.get("new-resp-header").contains(Seq("true")))
        Done
      })
    }

  }

}
