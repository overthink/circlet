package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.Circlet.handler
import com.markfeeney.circlet.TestUtils.complete
import com.markfeeney.circlet.{Request, Response}
import org.scalatest.FunSuite

class CookiesTest extends FunSuite {

//  private def assertResp(f: Response => Unit): Cont = {
//    case Some(resp) => f(resp); Sent
//    case None => fail("expected response")
//  }

//  private def assertNoResp: Cont = {
//    case Some(resp) => fail(s"did not expect response, got $resp")
//    case None => assert(true, "expected non-match"); Sent
//  }

  test("cooking parsing sanity") {
    val h = handler { req =>
      val cookies = Cookies.get(req)
      assert(cookies.isDefined)
      assert(cookies.get("foo") == "bar")
      Response()
    }
    val h0 = Cookies.mw(h)
    h0(Request.mock("/foo").addHeader("Cookie", "foo=bar"))(complete)
  }

}
