/*
 * The MIT License
 *
 * Copyright (c) 2022 Vladimir Bragin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.techlook.net.client.scala.utils

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class NetgymUtilsTest extends AsyncFlatSpec with AsyncIOSpec with Matchers {
  "base url for an empty list" should "not be defined" in {
    NetgymUtils.baseUrlFor[IO](Seq()).asserting(_ shouldBe empty)
  }

  "base url for a list with a single url" should "be appropriate for this url" in {
    val url = "protocol://host.name/path/to/resource?param=value"
    NetgymUtils.baseUrlFor[IO](Seq(url)).asserting(_ shouldBe Some("protocol://host.name/path/to/"))
  }

  "base url assembly" should "contain a whole part when integer url parts match" in {
    val urls = Seq(
      "protocol://www.domain.org/common/path/first/sub/",
      "protocol://www.domain.org/common/path/first/sub/resource.txt",
      "protocol://www.domain.org/common/path/firstpath/sub/request?param1=value1&param2=value2",
      "protocol://www.domain.org/common/path/second/path/request?param1=value1&param2=value2"
    )

    NetgymUtils.baseUrlFor[IO](urls).asserting(_ shouldBe Some("protocol://www.domain.org/common/path/"))
  }

  "base url assembly" should "contain a common part when partial url parts match" in {
    val urls = Seq(
      "protocol://www.domain.org/common/path/first/sub/",
      "protocol://www.domain.org/common/path/first/sub/resource.txt",
      "protocol://www.domain.org/common/path/firstpath/sub/request?param1=value1&param2=value2",
      "protocol://www.domain.org/common/path/firstpath/request?param1=value1&param2=value2"
    )

    NetgymUtils.baseUrlFor[IO](urls).asserting(_ shouldBe Some("protocol://www.domain.org/common/path/"))
  }

  "when hosts are different test, there" should "be no base url" in {
    val urls = Seq(
      "protocol://www.domain1.org/common/path/first/sub/",
      "protocol://www.domain2.org/common/path/first/sub/resource.txt",
      "protocol://www.domain3.org/common/path/firstpath/sub/request?param1=value1&param2=value2",
      "protocol://www.domain4.org/common/path/firstpath/request?param1=value1&param2=value2"
    )

    NetgymUtils.baseUrlFor[IO](urls).asserting(_ shouldBe empty)
  }

  "common domain" should "be the same" in {
    val urls = Seq(
      "protocol://www.domain.org",
      "protocol://www.domain.org/common/path/first/sub/resource.txt",
      "protocol://www.domain.org",
      "protocol://www.domain.org/"
    )

    NetgymUtils.baseUrlFor[IO](urls).asserting(_ shouldBe Some("protocol://www.domain.org/"))
  }

  "an exception" should "be thrown when there is a bad url in list" in {
    val urls = Seq(
      "protocol://www.domain.org",
      "protocol://www.domain.org/common/path/first/sub/resource.txt",
      "bad/url",
      "protocol://www.domain.org/"
    )

    NetgymUtils.baseUrlFor[IO](urls).assertThrows[IllegalArgumentException]
  }
}
