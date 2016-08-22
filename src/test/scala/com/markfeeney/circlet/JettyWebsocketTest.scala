package com.markfeeney.circlet

import java.util.concurrent.TimeUnit

import com.markfeeney.circlet.TestUtils.TestJettyServer
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.scalatest.FunSuite


class JettyWebsocketTest extends FunSuite {

  private val echoUpper = JettyWebSocket(
    onText = (session, msg) => session.getRemote.sendString(msg.toUpperCase)
  )

  private val echoLower = JettyWebSocket(
    onText = (session, msg) => session.getRemote.sendString(msg.toLowerCase)
  )

  private val dummyHandler = Circlet.handler(Response(body = "hi"))

  private def withWs[T](urlString: String)(f: (TestWebSocket, Session) => T): T = {
    var result: Option[T] = None // I am a terrible person
    Cleanly(new WebSocketClient())(_.stop) { client =>
      client.start()
      Cleanly(new TestWebSocket(urlString))(_.awaitClose()) { testWs =>
        Cleanly(client.connect(testWs, testWs.uri).get(2, TimeUnit.SECONDS))(_.close()) { session =>
          result = Some(f(testWs, session))
        }
      }
    }
    result.get
  }

  test("single message to single websocket works") {
    val opts0 = JettyOptions(webSockets = Map("/ws/" -> echoUpper))
    TestUtils.testServer(dummyHandler, opts0) { case TestJettyServer(_, opts) =>
      withWs(f"ws://localhost:${opts.httpPort}%d/ws/") { (ws, session) =>
        session.getRemote.sendString("this is a test!")
        assert(ws.nextStringMessage == "THIS IS A TEST!")
      }
    }
  }

  test("multiple messages to single websocket works") {
    val opts0 = JettyOptions(webSockets = Map("/foo/" -> echoUpper))
    TestUtils.testServer(dummyHandler, opts0) { case TestJettyServer(_, opts) =>
      withWs(f"ws://localhost:${opts.httpPort}%d/foo/") { (ws, session) =>
        session.getRemote.sendString("this is a test!")
        session.getRemote.sendString("AND another")
        assert(ws.nextStringMessage == "THIS IS A TEST!")
        assert(ws.nextStringMessage == "AND ANOTHER")
      }
    }
  }

  test("multiple websockets") {
    val opts0 = JettyOptions(webSockets = Map(
      "/upper/" -> echoUpper,
      "/lower/" -> echoLower
    ))

    TestUtils.testServer(dummyHandler, opts0) { case TestJettyServer(_, opts) =>
      val upperWs: TestWebSocket =
        withWs(f"ws://localhost:${opts.httpPort}%d/upper/") { (upper, session) =>
          session.getRemote.sendString("foo")
          session.getRemote.sendString("bar baz")
          upper
        }

      val lowerWs: TestWebSocket =
        withWs(f"ws://localhost:${opts.httpPort}%d/lower/") { (lower, session) =>
          session.getRemote.sendString("FOO")
          session.getRemote.sendString("BAR BAZ")
          lower
        }

      assert(upperWs.nextStringMessage == "FOO")
      assert(upperWs.nextStringMessage == "BAR BAZ")
      assert(lowerWs.nextStringMessage == "foo")
      assert(lowerWs.nextStringMessage == "bar baz")
    }
  }

}
