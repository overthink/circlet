package com.markfeeney.circlet

import scala.language.implicitConversions

/**
 * A type representing a value that is either a String or a Seq of String.
 * This is useful when parsing query string and form parameters, and creating
 * Response headers.
 */
sealed trait StrVal {
  /**
   * Get the value as a string, regardless of its actual type.  Wanted to leave toString
   * as its usual impl.
   */
  def asString: String
}

object StrVal {

  final case class Single(value: String) extends StrVal {
    def asString: String = value
  }

  final case class Multi(value: Seq[String]) extends StrVal {
    def asString: String = value.mkString(",")
  }

  implicit def strToRhv(s: String): StrVal = {
    Single(s)
  }

}

