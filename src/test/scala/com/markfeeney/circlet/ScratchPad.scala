package com.markfeeney.circlet

import com.markfeeney.circlet.Circlet.handler
import com.markfeeney.circlet.ResponseBody.SeqBody
import com.markfeeney.circlet.middleware._
import org.eclipse.jetty.websocket.api.Session
import org.joda.time.Duration

import scala.collection.mutable
import scala.util.Random

object ScratchPad {

  def lazySeq(): Unit = {
    val app: Handler = Circlet.handler { request =>
      Response(body = Some(SeqBody(1 to Int.MaxValue)))
    }

    val opts = JettyOptions(httpPort = 8888)
    JettyAdapter.run(app, opts)
  }

  def cpsLazy(): Unit = {
    val app: Handler = req => k => {
      println("create expensive thing")
      val xs = 1 to 50000000
      val resp = Response(body = Some(SeqBody(xs)))
      val result = k(resp)
      println("cleanup expensive thing")
      result
    }

    val filter: Middleware = handler => req => k => {
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
    val mw: Middleware = Head.mw
      .andThen(Params.mw())
      .andThen(MultipartParams.mw())
      .andThen(Cookies.mw())

    val h: Handler = handler { req =>
      Cookies.get(req, "id") match {
        case None =>
          val id = Random.nextInt(1000000)
          val body = s"No id yet, going to set $id (5 second ttl)"
          val c = Cookie(value = id.toString, maxAge = Some(new Duration(5000)))
          Cookies.add(Response(body = body), "id", c)
        case Some(id) =>
          Response(body = s"Id is $id")
      }
    }

    val app = mw(h)

    sys.addShutdownHook(println("in shutdown hook"))

    val opts = JettyOptions(httpPort = 8888)
    JettyAdapter.run(app, opts)
  }

  def trivial(): Unit = {
    JettyAdapter.run(handler(Response(body = "yo")), JettyOptions(httpPort = 8888))
  }

  def websocketTest(): Unit = {

    val sessions = new mutable.HashSet[Session]
    val pinger = new Thread {
      override def run(): Unit = {
        while(true) {
          val sleepTime = Random.nextInt(5000)
          sessions.synchronized {
            println(f"connected users: ${sessions.size}%d")
            sessions.foreach { s =>
              s.getRemote.sendString(f"ping - will now sleep for $sleepTime%d ms")
            }
          }
          Thread.sleep(sleepTime)
        }
      }
    }
    pinger.setName("pinger")
    pinger.setDaemon(true)
    pinger.start()

    val ws = JettyWebSocket(
      onConnect = s => {
        println(s"${s.getRemoteAddress}: connected")
        sessions.synchronized { sessions.add(s) }
      },
      onError = (s, t) => println(s"${s.getRemoteAddress}: got throwable: $t"),
      onClose = (s, code, reason) => {
        sessions.synchronized { sessions.remove(s) }
        println(s"${s.getRemoteAddress}: closed, code: $code, reason: $reason")
      },
      onText = (s, data) => s.getRemote.sendString(data.toUpperCase),
      onBytes = (_, _, _, _) => sys.error("bytes not supported")
    )

    val opts = JettyOptions(httpPort = 8888, webSockets = Map("/ws" -> ws, "/foobar" -> ws))
    JettyAdapter.run(handler(Response(body = "see /ws/")), opts)
  }

  def main(args: Array[String]) = websocketTest()

}
