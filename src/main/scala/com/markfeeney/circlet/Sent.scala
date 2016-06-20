package com.markfeeney.circlet

/**
 * Signals a request is fully processed and response fully sent.
 * In a perfect world this would be returned only by server adapters
 * when they're done sending the response.  But it's useful any time
 * you want to see the result of a handler executing (e.g. in tests).
 */
object Sent
