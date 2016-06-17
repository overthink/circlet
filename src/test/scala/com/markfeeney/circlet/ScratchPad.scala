package com.markfeeney.circlet

import com.markfeeney.circlet.CpsConverters._
import com.markfeeney.circlet.ResponseBody.SeqBody
import com.markfeeney.circlet.middleware.{ContentType, Head, MultipartParams, Params}

object ScratchPad {

  def lazySeq(): Unit = {
    val app: Handler = request => {
      Response(body = Some(SeqBody(1 to Int.MaxValue)))
    }

    val opts = JettyOptions(httpPort = 8888)
    JettyAdapter.run(app, opts)
  }

  def cpsLazy(): Unit = {
    val app: CpsHandler = req => k => {
      println("create expensive thing")
      val xs = 1 to 50000000
      val resp = Response(body = Some(SeqBody(xs)))
      val result = k(resp)
      println("cleanup expensive thing")
      result
    }

    val filter: CpsMiddleware = handler => req => k => {
      handler(req) { resp =>
          println("filter begin")
          val newBody = resp.flatMap(_.body) match {
            case Some(SeqBody(xs)) => Some(SeqBody(xs.take(100)))
            case other => other
          }
          val result = k(resp.map(_.copy(body = newBody)))
          println("filter end")
          result
      }
    }

    val opts = JettyOptions(httpPort = 8888)
    JettyAdapter.run(filter(app), opts)
  }

  def mwComposition(): Unit = {
    val mw: CpsMiddleware = ContentType()
      .andThen(Head.wrapCps)
      .andThen(Params.wrap())
      .andThen(MultipartParams.wrapCps())

    val handler: CpsHandler = req => k => k(Response(body = "Foo bar!\n"))

    val app = mw(handler)

    val opts = JettyOptions(httpPort = 8888)
    JettyAdapter.run(app, opts)

  }

  def main(args: Array[String]) = mwComposition()

}
