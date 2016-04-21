package com.markfeeney.circlet

sealed trait ResponseHeaderValue
object ResponseHeaderValue {
  case class Single(value: String) extends ResponseHeaderValue
  case class Multi(values: Seq[String]) extends ResponseHeaderValue
}
