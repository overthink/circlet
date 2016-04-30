package com.markfeeney.circlet

import org.eclipse.jetty.util.log.Logger

/** Used to disable Jetty logging in tests. */
class NoJettyLogging extends Logger {
  override def warn(msg: String, args: AnyRef*): Unit = {}
  override def warn(thrown: Throwable): Unit = {}
  override def warn(msg: String, thrown: Throwable): Unit = {}
  override def getName: String = "no-logging"
  override def isDebugEnabled: Boolean = false
  override def getLogger(name: String): Logger = this
  override def ignore(ignored: Throwable): Unit = {}
  override def debug(msg: String, args: AnyRef*): Unit = {}
  override def debug(msg: String, value: Long): Unit = {}
  override def debug(thrown: Throwable): Unit = {}
  override def debug(msg: String, thrown: Throwable): Unit = {}
  override def setDebugEnabled(enabled: Boolean): Unit = {}
  override def info(msg: String, args: AnyRef*): Unit = {}
  override def info(thrown: Throwable): Unit = {}
  override def info(msg: String, thrown: Throwable): Unit = {}
}
