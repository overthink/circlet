package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.TestUtils.{hwApp, request}
import com.markfeeney.circlet.{Handler, HttpMethod}
import org.scalatest.FunSuite

class ContentTypeTest extends FunSuite {

  private val wrapped: Handler = ContentType.wrap()(hwApp)

  private def contentType(path: String): Option[Vector[String]] = {
    wrapped(request(HttpMethod.Get, path)).headers.get("Content-Type")
  }

  test("known extension gets correct content type") {
    println(contentType("/fluffhead.png"))
    assert(contentType("/fluffhead.png").contains(Vector("image/png")))
  }

  test("unknown extension gets default content type") {
    assert(contentType("/fluffhead.bogus-extension").contains(Vector("application/octet-stream")))
  }

  test("extension case is ignored") {
    val ct1 = contentType("/fluffhead.mp4")
    val ct2 = contentType("/fluffhead.MP4")
    assert(ct1 == ct2)
    assert(ct1.contains(Vector("video/mp4")))
  }

}
