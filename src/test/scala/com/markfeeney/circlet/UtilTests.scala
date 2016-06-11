package com.markfeeney.circlet

import java.nio.charset.StandardCharsets.{UTF_8, UTF_16}

import org.scalatest.FunSuite

class UtilTests extends FunSuite {

  test("form decode to a string") {
    def decode(s: String): Option[String] = Util.formDecodeString(s, UTF_8)
    assert(decode("hello").contains("hello"))
    assert(decode("").contains(""))
    assert(decode("%22hi%20%26%20stuff%22").contains("\"hi & stuff\""))
    assert(decode("b%2fc%2Fd").contains("b/c/d"), "mixed case works for hex values")
    assert(Util.formDecodeString("foo%FE%FF%00%2Fbar", UTF_16).contains("foo/bar"))
    assert(decode("a%zzb").isEmpty, "zz isn't valid hex, should explode")
  }

  test("form decode to a map") {
    def decode(s: String): Map[String, Vector[String]] = Util.formDecodeMap(s, UTF_8)
    assert(decode("") == Map.empty)
    assert(decode("a") == Map("a" -> Vector.empty[String]))
    assert(decode("a=b") == Map("a" -> Vector("b")))
    assert(decode("a=b&c=d%2fe") == Map("a" -> Vector("b"), "c" -> Vector("d/e")))
    assert(decode("a=b&c=%zz") == Map("a" -> Vector("b"), "c" -> Vector.empty[String]))
    assert(decode("a=b&c") == Map("a" -> Vector("b"), "c" -> Vector.empty[String]))
    assert(decode("a=b&") == Map("a" -> Vector("b")))
    assert(Util.formDecodeMap("a=foo%FE%FF%00%2Fbar", UTF_16) == Map("a" -> Vector("foo/bar")))
  }

}
