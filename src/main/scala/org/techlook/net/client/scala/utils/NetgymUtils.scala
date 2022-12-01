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

import cats.data.OptionT
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import cats.{ Applicative, ApplicativeError, Monad, MonadError }
import org.techlook.net.client.http.Pair

import java.util.regex.Pattern
import scala.collection.Factory
import scala.jdk.CollectionConverters._

object NetgymUtils {
  def toJava(keyValues: Map[String, String]): java.util.Set[Pair[String, String]] = keyValues
    .map { case (key, value) => new Pair(key, value) }
    .toSet
    .asJava

  def toScala(keyValues: java.util.Set[Pair[String, String]]): Map[String, String] = keyValues
    .asScala
    .map(pair => pair.getKey -> pair.getValue)
    .toMap

  def toScala(keyValues: java.util.Map[String, String]): Map[String, String] = keyValues
    .asScala
    .toMap

  def baseUrlFor[F[_] : MonadError[*[_], Throwable]](iterable: Iterable[String]): F[Option[String]] = {
    iterable match {
      case urls if urls.isEmpty => Applicative[F].pure(none[String])

      case urls =>
        val listOfUrls = urls.toList

        val baseUrlMatch = Monad[F].tailRecM(listOfUrls.head -> listOfUrls.tail) {
          case (baseUrl, head :: tail) => Some(serverPart.matcher(head))
            .filter(_.find())
            .map { _ =>
              baseUrl
                .zip(head)
                .takeWhile { case (left, right) => left == right }
                .map { case (char, _) => char }
                .to(Factory.stringFactory)
            }
            .map(_ -> tail)
            .map(_.asLeft[Option[String]].pure[F])
            .getOrElse {
              ApplicativeError[F, Throwable].raiseError(new IllegalArgumentException(s"bad URL specified: $head"))
            }

          case (baseUrl, Nil) => baseUrl.some.asRight[(String, List[String])].pure[F]
        }

        OptionT(baseUrlMatch)
          .map { baseUrl =>
            Some(baseUrl)
              .filter { url =>
                val matcher = serverPart.matcher(url)
                matcher.find() && listOfUrls.head == matcher.group(1)
              }
              .map(_ + '/')
              .getOrElse(baseUrl.take(baseUrl.lastIndexOf('/') + 1))
          }
          .filterNot(protocolOnly.matcher(_).matches())
          .value
    }
  }

  private lazy val protocolOnly: Pattern = Pattern.compile("^\\w+://$")
  private lazy val serverPart: Pattern = Pattern.compile("(^\\w+://[^/]+)")
}
