package com.markfeeney.circlet

import java.net.URI
import java.util.concurrent.{ArrayBlockingQueue, CountDownLatch, TimeUnit}

import org.eclipse.jetty.websocket.api.WebSocketAdapter

/** WebSocket that queues any string messages received. */
class TestWebSocket(wsUrlString: String) extends WebSocketAdapter {

  val uri = new URI(wsUrlString)
  private val messages = new ArrayBlockingQueue[String](32)
  private val closeLatch = new CountDownLatch(1)

  /** Returns the next string message on the socket, waiting a few seconds
   * if necessary. */
  def nextStringMessage: String = {
    // Wait briefly; assumes most tests don't have complicated timing situations.
    val result = messages.poll(2, TimeUnit.SECONDS)
    if (result == null) {
      sys.error(s"$uri - Timed out waiting for message from websocket")
    }
    result
  }

  override def onWebSocketText(message: String): Unit = {
    messages.put(message)
  }

  override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
    closeLatch.countDown()
  }

  def awaitClose(): Unit = {
    closeLatch.await(2, TimeUnit.SECONDS)
  }

}
