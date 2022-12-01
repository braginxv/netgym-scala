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

import cats.effect.Async
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.monadError._
import cats.{ FlatMap, Functor, MonadError }
import fs2._
import io.circe
import io.circe.parser._
import io.circe.{ Decoder, Json, ParsingFailure }
import org.techlook.net.client.ClientSystem
import org.techlook.net.client.http._
import org.techlook.net.client.http.adapters.{ ByteResponseListener, Response }
import org.techlook.net.client.scala.utils.NetgymUtils

import java.net.{ HttpURLConnection, URL }
import java.nio.charset.StandardCharsets
import javax.net.ssl.{ KeyManager, TrustManager }

class NetgymHttpClient[F[_] : Async : FlatMap](
  baseUrl: URL,
  basicHeaders: Map[String, String] = Map.empty,
  clientKeyManagers: Iterable[KeyManager] = Iterable.empty,
  trustManagers: Iterable[TrustManager] = Iterable.empty,
  connectionLifetime: ConnectionLifetime = Pipelining,
  pipelineConnectionSendingInterval: Long = PipeliningConnection.DEFAULT_SENDING_INTERVAL
) {

  import NetgymHttpClient._

  private val connectionF = Async[F].delay(fromBaseUrl(baseUrl))

  private val headers = basicHeaders + (AGENT_HEADER -> AGENT_CLIENT)
  private val basePath = Some(baseUrl.getPath).filterNot(_.endsWith("/")).map(_ + '/').getOrElse(baseUrl.getPath)

  def acquire(request: HttpRequest): F[HttpResponse] = {
    for {
      connection <- connectionF

      response <- Async[F].async_ { callback: (Either[Throwable, HttpResponse] => Unit) =>
        request.request(connection, basePath, headers, new ByteResponseListener {
          override def respond(response: org.techlook.net.client.Either[String, Response]): Unit = {
            response.left().apply(errorMessage => callback(new RuntimeException(errorMessage).asLeft))

            response.right().apply {
              case httpResponse if httpResponse.getCode == HttpURLConnection.HTTP_OK =>
                callback(HttpResponse(
                  httpResponse.getCode,
                  httpResponse.getHttpVersion,
                  httpResponse.getResponseDescription,
                  NetgymUtils.toScala(httpResponse.getHeaders),
                  httpResponse.getContent,
                  Option(httpResponse.getCharset)
                ).asRight)

              case httpResponse =>
                val content = new String(httpResponse.getContent,
                  Option(httpResponse.getCharset).getOrElse(StandardCharsets.UTF_8))

                callback(Left(new RuntimeException(s"The server responded with code: ${httpResponse.getCode}\n$content")))
            }
          }
        })
      }
    } yield response
  }

  def acquire[K]: Pipe[F, (K, HttpRequest), (K, HttpResponse)] = {
    inbox =>
      inbox.evalMap { case (key, request) =>
        acquire(request).map(key -> _)
      }
  }

  def toByteResponses[K]: Pipe[F, (K, HttpResponse), (K, Array[Byte])] =
    _.map { case (key, response) => key -> response.content }

  def toStringResponses[K]: Pipe[F, (K, HttpResponse), (K, String)] =
    _.map { case (key, response) => key -> response.asString }

  def toJsonResponses[K]: Pipe[F, (K, HttpResponse), (K, Json)] = _
    .map { case (key, response) => response.asJson.map(key -> _) }
    .rethrow

  def toObjectResponses[K, T: Decoder]: Pipe[F, (K, HttpResponse), (K, T)] = _
    .map { case (key, response) => response.asObject[T].map(key -> _) }
    .rethrow

  protected[scala] def fromBaseUrl(baseUrl: URL): HttpConnection = {
    val port = Some(baseUrl.getPort).getOrElse(baseUrl.getDefaultPort)

    val sslKeyManagers = if (clientKeyManagers.isEmpty) null else clientKeyManagers.toArray
    val sslTrustManagers = if (trustManagers.isEmpty) null else trustManagers.toArray

    val networkClient = baseUrl.getProtocol match {
      case HTTP => ClientSystem.client()
      case HTTPS => ClientSystem.sslClient(sslKeyManagers, sslTrustManagers)
    }

    connectionLifetime match {
      case Closable => new SingleConnection(baseUrl.getHost, port, networkClient)
      case Consecutive => new SequentialConnection(baseUrl.getHost, port, networkClient)
      case Pipelining => new PipeliningConnection(baseUrl.getHost, port, networkClient,
        pipelineConnectionSendingInterval)
    }
  }
}

object NetgymHttpClient {
  final val HTTP: String = "http"
  final val HTTPS: String = "https"
  final val AGENT_HEADER: String = "User-Agent"
  final val AGENT_CLIENT: String = "Netgym network library (https://github.com/braginxv/netgym)"

  implicit class ResponseConversions(response: HttpResponse) {
    def asString: String =
      new String(response.content, response.charset.getOrElse(StandardCharsets.UTF_8))

    def asJson: Either[ParsingFailure, Json] = parse(asString)

    def asObject[T: Decoder]: Either[circe.Error, T] = asJson.flatMap(_.as[T])
  }

  implicit class ResponseConversionsF[F[_]: Functor: MonadError[*[_], Throwable]](response: F[HttpResponse]) {
    def asString: F[String] = response.map(_.asString)

    def asJson: F[Json] = response.map(_.asJson).rethrow

    def asObject[T: Decoder]: F[T] = response.map(_.asObject[T]).rethrow
  }
}
