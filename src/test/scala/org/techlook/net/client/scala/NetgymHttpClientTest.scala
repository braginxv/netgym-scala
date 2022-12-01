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

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Decoder
import io.circe.generic.semiauto._
import org.scalamock.scalatest._
import org.scalatest.Assertion
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.techlook.net.client.http.client.HttpListener
import org.techlook.net.client.http.{ HttpConnection, Pair }
import org.techlook.net.client.scala.NetgymHttpClient._
import org.techlook.net.client.scala.utils.NetgymUtils._

import java.net.URL
import java.nio.charset.{ Charset, StandardCharsets }
import java.util

class NetgymHttpClientTest extends AsyncFlatSpec with AsyncIOSpec with AsyncMockFactory with Matchers {
  private val host = "www.host.org"
  private val startPath = "base/path"
  private val baseUrl = s"http://$host/$startPath/"
  private val pathToResource = "path/to/resource"
  private val contentType = "text/plain"
  private val requestBody =
    """
      |content
      |to be sent
      |""".stripMargin
  private val basicHeaders = Map(
    "Basic header 1" -> "value1",
    "Basic header 2" -> "value2",
    "Basic header 3" -> "value3"
  )
  private val requestHeaders = Map(
    "Request header 1" -> "value1",
    "Request header 2" -> "value2",
    "Request header 3" -> "value3"
  )
  private val parameters = Map(
    "param1" -> "value1",
    "param2" -> "value2",
    "param3" -> "value3"
  )

  def withHttpClient(test: (NetgymHttpClient[IO], HttpConnection) => IO[Assertion]): IO[Assertion] = {
    val connection = mock[HttpConnection]

    val client = new NetgymHttpClient[IO](new URL(baseUrl),
      basicHeaders = basicHeaders, connectionLifetime = Pipelining) {
      override protected[scala] def fromBaseUrl(baseUrl: URL): HttpConnection = connection
    }

    test(client, connection)
  }

  it should "be a correct HTTP request" in withHttpClient { (httpClient, connection) =>
    case class User(id: Long, family: String, name: String)
    implicit val UserDecoder: Decoder[User] = deriveDecoder

    val testUser = User(12345, "Jenkins", "Jerry")

    (connection.postContent _).expects {
      where { (actualBaseUrl: String, actualHeaders: java.util.Set[Pair[String, String]],
        actualParameters: java.util.Set[Pair[String, String]], actualContentType: String, actualCharset: Charset,
        actualBody: Array[Byte], listener: HttpListener) =>

        val buffer =
          s"""
             |{
             |  "id": ${testUser.id},
             |  "family": "${testUser.family}",
             |  "name": "${testUser.name}"
             |}
             |""".stripMargin.getBytes()

        listener.responseCode(200, "HTTP/1.1", null)
        listener.respondHeaders(new util.HashMap[String, String](){
          put("Content-Type", "application/json;charset=utf-8")
          put("Content-Length", buffer.length.toString)
        })
        listener.respond(buffer)
        listener.complete()

        actualBaseUrl == s"/$startPath/$pathToResource" &&
          (basicHeaders ++ requestHeaders + (NetgymHttpClient.AGENT_HEADER -> NetgymHttpClient.AGENT_CLIENT)) ==
            toScala(actualHeaders) &&
          parameters == toScala(actualParameters) &&
          contentType == actualContentType &&
          StandardCharsets.UTF_8 == actualCharset &&
          util.Arrays.equals(requestBody.getBytes(), actualBody)
      }
    }.once()

    httpClient.acquire(POST(pathToResource, requestBody, contentType, additionalHeaders = requestHeaders,
      parameters = parameters, charset = StandardCharsets.UTF_8))
      .asObject[User]
      .asserting { user =>
        user should equal(testUser)
      }
  }
}
