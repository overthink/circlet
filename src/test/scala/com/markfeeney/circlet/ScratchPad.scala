package com.markfeeney.circlet

import com.markfeeney.circlet.CpsConverters._
import com.markfeeney.circlet.ResponseBody.SeqBody

object ScratchPad {

  def lazySeq(): Unit = {
    val app: Handler = request => {
      Response(body = Some(SeqBody(1 to Int.MaxValue)))
    }

    val opts = JettyOptions(httpPort = 8888)
    JettyAdapter.run(app, opts)
  }

  def cpsLazy(): Unit = {
    val app: CpsHandler = (req, k) => {
      println("create expensive thing")
      val xs = 1 to 50000000
      val resp = Response(body = Some(SeqBody(xs)))
      val result = k(resp)
      println("cleanup expensive thing")
      result
    }

    val filter: CpsMiddleware = handler => (req, k) => {
      handler(req, resp => {
        println("filter begin")
        val newBody = resp.body match {
          case Some(SeqBody(xs)) => Some(SeqBody(xs.take(100)))
          case other => other
        }
        val result = k(resp.copy(body = newBody))
        println("filter end")
        result
      })
    }

    val opts = JettyOptions(httpPort = 8888)
    JettyAdapter.run(filter(app), opts)

  }

  def main(args: Array[String]) = cpsLazy()

}
