package com.markfeeney.poise

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.scalatest.FunSuite

class RouteTest extends FunSuite {

  private def t(path: String, url: String) = Route.parse(Route.compile(path), url)

  test("empty route is invalid") {
    intercept[ParseCancellationException](Route.compile(""))
  }

  test("invalid chars in route") {
    intercept[ParseCancellationException](Route.compile("/a b/c"))
    intercept[ParseCancellationException](Route.compile("/a\tb"))
  }

  test("static routes") {
    def t(path: String) = Route.parse(Route.compile(path), path)
    assert(t("/") == Map.empty)
    assert(t("/foo") == Map.empty)
    assert(t("/foo/bar") == Map.empty)
    assert(t("/foo/bar.html") == Map.empty)
  }

  test("route param extraction") {
    def t(path: String): Vector[String] = {
      val r = Route.compile(path)
      assert(r.path == path)
      r.paramNames
    }
    assert(t("/") == Vector.empty)
    assert(t("/foo") == Vector.empty)
    assert(t("/foo/bar") == Vector.empty)
    assert(t("/foo/:id") == Vector("id"))
    assert(t("/foo/:id/bar") == Vector("id"))
    assert(t("/foo/:id/bar/:bar-id") == Vector("id", "bar-id"))
    assert(t("/foo/*/bar/:id/*/:id2/b") == Vector("*", "id", "*", "id2"))
    assert(t("/:x/:x/:y/:x") == Vector("x", "x", "y", "x"))
    assert(t("/foo/:x.y") == Vector("x.y"))
    assert(t("/:ä/:ش") == Vector("ä", "ش"))
    assert(t("/foo%20bar/:baz") == Vector("baz"))
  }

}
