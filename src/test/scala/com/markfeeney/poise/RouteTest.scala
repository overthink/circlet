package com.markfeeney.poise

import org.antlr.v4.runtime.misc.ParseCancellationException
import org.scalatest.FunSuite

class RouteTest extends FunSuite {

  private def t(path: String, url: String) = Route.parse(Route.compile(path), url)

  test("empty route is invalid") {
    intercept[ParseCancellationException](Route.compile(""))
  }

  test("static routes") {
    def t(path: String) = Route.parse(Route.compile(path), path)
    assert(t("/") == Map.empty)
    assert(t("/foo") == Map.empty)
    assert(t("/foo/bar") == Map.empty)
    assert(t("/foo/bar.html") == Map.empty)
  }

  ignore("parser debugging") {
    def t(s: String) = Route.compile(s)
    t("/")
    t("/foo")
    t("/foo/bar")
    t("/foo/:id")
    t("/foo/:id/bar")
    t("/foo/:id/bar/:barid")
    t("/foo/*/bar/:id/a/:id2/b")
  }

}
