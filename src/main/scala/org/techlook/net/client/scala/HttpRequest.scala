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
import org.techlook.net.client.http.client.HttpListener
import org.techlook.net.client.http.{ FormRequestData, HttpConnection }
import org.techlook.net.client.scala.utils.NetgymUtils

import java.nio.charset.{ Charset, StandardCharsets }

sealed trait HttpRequest {
  protected[scala] def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener): Unit
}

object HttpRequest {
  private[scala] def toBuffer[T](body: T, charset: Charset) = {
    body match {
      case buffer: Array[Byte] => buffer
      case content: String => content.getBytes(charset)
      case json: Json => json.noSpaces.getBytes(charset)
      case other => other.toString.getBytes(charset)
    }
  }
}

case class GET(
  path: String,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.get(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters), listener)
  }
}

case class HEAD(
  path: String,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.head(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters), listener)
  }
}

case class TRACE(
  path: String,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.trace(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters), listener)
  }
}

case class OPTIONS(
  path: String,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.optionsWithUrl(s"$basePath$path",
      NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters), listener)
  }
}

case class SERVER_OPTIONS(additionalHeaders: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.options(NetgymUtils.toJava(basicHeaders ++ additionalHeaders), listener)
  }
}

case class CONNECT(
  path: String,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.connect(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters), listener)
  }
}

case class PUT[+T](
  path: String,
  body: T,
  contentType: String,
  charset: Charset = StandardCharsets.UTF_8,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.put(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters),
      contentType, charset, HttpRequest.toBuffer(body, charset), listener)
  }
}

case class DELETE[+T](
  path: String,
  body: T,
  contentType: String,
  charset: Charset = StandardCharsets.UTF_8,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.delete(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters),
      contentType, charset, HttpRequest.toBuffer(body, charset), listener)
  }
}

case class PATCH[+T](
  path: String,
  body: T,
  contentType: String,
  charset: Charset = StandardCharsets.UTF_8,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.patch(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters),
      contentType, charset, HttpRequest.toBuffer(body, charset), listener)
  }
}

case class POST[+T](
  path: String,
  body: T,
  contentType: String,
  charset: Charset = StandardCharsets.UTF_8,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.postContent(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), NetgymUtils.toJava(parameters),
      contentType, charset, HttpRequest.toBuffer(body, charset), listener)
  }
}

case class POST_ENCODED_PARAMETERS(
  path: String,
  additionalHeaders: Map[String, String] = Map.empty,
  parameters: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    httpClient.postWithEncodedParameters(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders),
      NetgymUtils.toJava(parameters), listener)
  }
}

case class POST_FORM_DATA(
  path: String,
  formData: List[FormEntry[Any]],
  additionalHeaders: Map[String, String] = Map.empty) extends HttpRequest {
  protected[scala] override def request(httpClient: HttpConnection, basePath: String, basicHeaders: Map[String, String], listener: HttpListener) {
    val form = formData.foldLeft(new FormRequestData) { (requestData, item) =>
      item.addToRequestData(requestData)
      requestData
    }

    httpClient.postFormData(s"$basePath$path", NetgymUtils.toJava(basicHeaders ++ additionalHeaders), form, listener)
  }
}


