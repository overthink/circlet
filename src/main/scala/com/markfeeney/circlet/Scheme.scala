package com.markfeeney.circlet

import java.util.Locale

sealed trait Scheme
object Scheme {
  case object Http extends Scheme
  case object Https extends Scheme

  def parse(s: String): Scheme = {
    s.toLowerCase(Locale.ENGLISH) match {
      case "http" => Http
      case "https" => Https
      case other => sys.error(s"Could not parse '$other' into a Scheme instance")
    }
  }
}
