package com.markfeeney.circlet

/**
 * Do a thing with guaranteed cleanup: http://stackoverflow.com/a/8865994/69689
 * aka Using
 * aka ARM
 * aka try-with-resource
 */
object Cleanly {
  def apply[A, B](resource: => A)(cleanup: A => Unit)(code: A => B): Either[Exception, B] = {
    try {
      val r = resource
      try { Right(code(r)) } finally { cleanup(r) }
    }
    catch {
      case e: Exception => Left(e)
    }
  }
}
