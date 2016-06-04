package com.markfeeney.circlet

/**
 * Returned by server adapters when they're done sending the response.
 *
 * This is private to the circlet package since the only code that should ever
 * be able to know if a response is fully sent is a server adapter.
 */
private[circlet] object Sent
