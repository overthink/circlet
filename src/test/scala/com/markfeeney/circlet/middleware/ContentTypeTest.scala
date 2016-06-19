package com.markfeeney.circlet.middleware

import com.markfeeney.circlet.TestUtils.{hwApp, request}
import com.markfeeney.circlet.{Handler, HttpMethod}
import com.markfeeney.circlet.Circlet.extractResponse
import org.scalatest.FunSuite

class ContentTypeTest extends FunSuite {

  private val wrapped: Handler = ContentType.mw()(hwApp)

  private def contentType(path: String): Option[Vector[String]] = {
    extractResponse(wrapped(request(HttpMethod.Get, path))).flatMap(_.contentType)
  }

  test("known extension gets correct content type") {
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
