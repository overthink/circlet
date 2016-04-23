package com.markfeeney.circlet

sealed trait ResponseHeaderValue {
  /**
   * Get the value as a string, regardless of its actual type.  Wanted to leave toString
   * as its usual impl.
   */
  def asString: String
}
object ResponseHeaderValue {
  case class Single(value: String) extends ResponseHeaderValue {
    def asString: String = value
  }
  case class Multi(values: Seq[String]) extends ResponseHeaderValue {
    def asString: String = values.mkString(",")
  }
}
