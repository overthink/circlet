package com.markfeeney.circlet

import java.net.URI
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.markfeeney.circlet.TestUtils.TestJettyServer
import org.eclipse.jetty.websocket.api.{Session, WebSocketAdapter}
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.scalatest.FunSuite


class JettyWebsocketTest extends FunSuite {

  private val echoUpper = JettyWebSocket(
    onText = (session, msg) => session.getRemote.sendString(msg.toUpperCase)
  )

  private val dummyHandler = Circlet.handler(Response(body = "hi"))

  private def withWsSession[T](uri: URI, wsa: WebSocketAdapter)(f: Session => T): T = {
    val client = new WebSocketClient()
    client.start()
    try {
      val session = client.connect(wsa, uri).get(1, TimeUnit.SECONDS)
      try {
        f(session)
      } finally {
        session.close()
      }
    } finally {
      client.stop()
    }
  }

  test("single message to single websocket works") {
    val opts0 = JettyOptions(webSockets = Map("/ws/" -> echoUpper))
    TestUtils.testServer(dummyHandler, opts0) { case TestJettyServer(_, opts) =>

      val latch = new CountDownLatch(1)
      var response: String = null
      val wsa = new WebSocketAdapter {
        override def onWebSocketText(message: String): Unit = {
          response = message
          latch.countDown()
        }
      }

      withWsSession(new URI(f"ws://localhost:${opts.httpPort}%d/ws/"), wsa) { session =>
        session.getRemote.sendString("this is a test!")
        assert(latch.await(1, TimeUnit.SECONDS))
        assert(response == "THIS IS A TEST!")
      }
    }
  }

}
