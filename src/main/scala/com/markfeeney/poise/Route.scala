package com.markfeeney.poise

trait Route {
  // e.g. "/foo/:fooId/bar/:barId"
  def path: String
}

object Route {

  sealed trait ParamValue {
    def value: String
  }
  final case class Single(value: String) extends ParamValue
  final case class Multiple(xs: Vector[String]) {
    assert(xs.nonEmpty, "Multiple must contain at least one value")
    def value: String = xs.head
  }

  case class Impl private(path: String) extends Route

  def compile(path: String): Route = {
    Impl(path)
  }

  def parse(route: Route, url: String): Map[String, ParamValue] = {
    Map.empty
  }

}
