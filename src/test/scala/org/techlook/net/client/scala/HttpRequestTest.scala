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

package org.techlook.net.client.scala

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import scala.util.Random

class HttpRequestTest extends AnyFlatSpec with Matchers {
  "byte buffer" should "be left as is" in {
    val contentLength = 255
    val content = Random.nextBytes(contentLength)

    HttpRequest.toBuffer(content, StandardCharsets.UTF_8) shouldBe content
  }

  "string content" should " be taken as a byte buffer" in {
    val content = "test\ncontent"

    HttpRequest.toBuffer(content, StandardCharsets.UTF_8) shouldBe content.getBytes(StandardCharsets.UTF_8)
  }

  "object" should " be rendered as its string representation" in {
    val content = List("test", "content")

    HttpRequest.toBuffer(content, StandardCharsets.UTF_8) shouldBe content.toString.getBytes(StandardCharsets.UTF_8)
  }

  "Json content" should "be rendered as a compact json" in {
    val content = Json.obj (
      "id" -> Json.fromInt(1234),
      "family" -> Json.fromString("Jenkins"),
      "name" -> Json.fromString("Build")
    )

    HttpRequest.toBuffer(content, StandardCharsets.UTF_8) shouldBe content.noSpaces.getBytes(StandardCharsets.UTF_8)
  }
}
