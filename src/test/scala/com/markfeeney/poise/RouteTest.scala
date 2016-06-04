package com.markfeeney.poise

import com.markfeeney.poise.Route.Single
import com.markfeeney.poise.parser.{RouteLexer, RouteParser}
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}
import org.scalatest.FunSuite

class RouteTest extends FunSuite {

  private def t(path: String, url: String) = Route.parse(Route.compile(path), url)

  test("static path") {
    def t(path: String) = Route.parse(Route.compile(path), path)
    assert(t("") == Map.empty)
    assert(t("/") == Map.empty)
    assert(t("/foo") == Map.empty)
    assert(t("/foo/bar") == Map.empty)
    assert(t("/foo/bar.html") == Map.empty)
  }

  test("basic param extraction") {
    assert(t("/:x", "/foo") == Map("x" -> Single("foo")))
  }

  test("antlr4 experiement") {
    val input = new ANTLRInputStream("/foo/:id/bar%20baz/*/abc")
    val lexer = new RouteLexer(input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new RouteParser(tokens)
    val tree = parser.route
    println("tree begin")
    println(tree.toStringTree(parser))
    println("tree end")
    assert(true)
  }

}
