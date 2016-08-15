package com.markfeeney.circlet

import org.eclipse.jetty.websocket.api.Session

/**
 * Handler for a websocket connection.  Jetty-specific.
 *
 * @param onConnect Called with live session after connect is done
 * @param onError Called with live session after an uncaught error
 * @param onClose Called right before the websocket is closed
 * @param onText Called when a text message arrives from client
 * @param onBytes Called when a binary message arrives from client
 */
case class JettyWebSocket(
  onConnect: Session => Unit = _ => (),
  onError: (Session, Throwable) => Unit = (_, _) => (),
  onClose: (Session, Int, String) => Unit = (_, _, _) => (), // no Session since it is null by the time this runs
  onText: (Session, String) => Unit = (_, _) => (),
  onBytes: (Session, Array[Byte], Int, Int) => Unit = (_, _, _, _) => ()
)
