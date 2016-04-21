package com.markfeeney.circlet

case class JettyOptions(
  join: Boolean = true,
  port: Int = 80,
  maxThreads: Int = 50
)
