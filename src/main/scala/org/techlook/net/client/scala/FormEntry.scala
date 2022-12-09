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

import org.techlook.net.client.http.FormRequestData

import java.nio.charset.{ Charset, StandardCharsets }

case class FormEntry[+T](name: String,
  body: T,
  contentType: String,
  charset: Charset = StandardCharsets.UTF_8,
  fileName: Option[String] = None) {

  private[scala] def addToRequestData(requestData: FormRequestData): Unit = (body, fileName) match {
    case (buffer: Array[Byte], Some(file)) =>
      requestData.addFileField(name, file, buffer, contentType, charset)

    case (content: String, Some(file)) =>
      requestData.addFileField(name, file, content.getBytes(charset), contentType, charset)

    case (otherContent, Some(file)) =>
      requestData.addFileField(name, file, otherContent.toString.getBytes(charset), contentType, charset)

    case (buffer: Array[Byte], None) =>
      requestData.addInputField(name, buffer, contentType, charset)

    case (content: String, None) =>
      requestData.addInputField(name, content, contentType, charset)

    case (otherContent, None) =>
      requestData.addInputField(name, otherContent.toString, contentType, charset)
  }
}
