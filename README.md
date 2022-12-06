
## Netgym Network Library for Scala
This library provides facilities for more convenient development with Netgym (high performance network library https://github.com/braginxv/netgym) in Scala.

To add the library to your project add "Sonatype Nexus snapshots" repository:
```sbt
resolvers +=  "Sonatype snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"
```
and
```sbt
libraryDependencies += "com.github.braginxv" %% "netgym-scala" % "0.5-SNAPSHOT"
```
or
```sbt
libraryDependencies += "com.github.braginxv" % "netgym-scala_2.13" % "0.5-SNAPSHOT"
```
Nowadays only Scala 2.13 is supported.

### Usage
The wrappers in this library suggest asynchronous network operations using Cats (https://github.com/typelevel/cats), Cats-effect (https://github.com/typelevel/cats-effect) and FS2 (https://github.com/typelevel/fs2) notations.
For instance, let's say you need to download a set of images from a remote server and save them in local storage. To do this you could write:

```scala
    val program = for {
      mayBeBaseUrl <- Stream.eval(NetgymUtils.baseUrlFor[IO](urls))

      baseUrl <- Stream.eval {
        IO.fromOption(mayBeBaseUrl)(new IllegalStateException("Images to be downloaded reside on different hosts"))
      }

      imagePaths = urls.map(_.replace(baseUrl, ""))
      httpClient = new NetgymHttpClient[IO](new URL(baseUrl))

      response <- Stream
        .iterable(imagePaths)
        .map(path => path.replaceFirst(".*?(?=[^/]+$)", "") -> GET(path))
        .through(httpClient.acquire)
        .evalTap { case (key, response) =>
          Some(response).filterNot(_.code == HttpURLConnection.HTTP_OK).traverse { response =>
            IO.println(
              s"""Download "$key" is completed with code: ${response.code}. No content for further processing""")
          }
        }
        .filter { case (_, response) => response.code == HttpURLConnection.HTTP_OK }
        .through(NetgymHttpClient.toByteResponses)

      (fileName, content) = response

      _ <- Stream
        .chunk(Chunk.array(content))
        .through(Files[IO].writeAll(Path(s"$outputDirectory/$fileName")))
    } yield ()

    program.compile.drain
```

You could also use ZIO (https://github.com/zio/zio) effect-system to perform http-requests. For this you should add to project ZIO's cats-interop (https://github.com/zio/interop-cats)
```sbt
libraryDependencies += "dev.zio" %% "zio-interop-cats" % "3.3.0"
```
and use zio.Task or other ZIO's effects instead of cats.effect.IO:

```scala
import zio._
import zio.interop.catz._

val program: Stream[Task, Unit] = for {
  mayBeBaseUrl <- Stream.eval(NetgymUtils.baseUrlFor[Task](urls))

  baseUrl <- Stream.eval[Task, String] {
    ZIO.fromOption(mayBeBaseUrl)
      .mapError(_ => new IllegalStateException("Images to be downloaded reside on different hosts"))
  }

  imagePaths = urls.map(_.replace(baseUrl, ""))
  httpClient = new NetgymHttpClient[Task](new URL(baseUrl))

  response <- Stream
    .iterable(imagePaths)
    .map(path => path.replaceFirst(".*?(?=[^/]+$)", "") -> GET(path))
    .through(httpClient.acquire)
    .evalTap { case (key, response) =>
        Some(response).filterNot(_.code == HttpURLConnection.HTTP_OK).traverse { response =>
          Console.printError(
            s"""Download "$key" is completed with code: ${response.code}. No content for further processing""")
        }
    }
    .filter { case (_, response) => response.code == HttpURLConnection.HTTP_OK }
    .through(NetgymHttpClient.toByteResponses)

  (fileName, content) = response

  _ <- Stream
    .chunk(fs2.Chunk.array(content))
    .through(Files[Task].writeAll(Path(s"$outputDirectory/$fileName")))
} yield ()

program.compile.drain
```

### Other stream convertors

There are other convertors allowing you to get http responses in a more appropriate form you might need

```scala
NetgymHttpClient.toByteResponses[F, KeyType] // returns Pipe[F, (KeyType, HttpResponse), (KeyType, Array[Byte])]

NetgymHttpClient.toStringResponses[F, KeyType] // returns Pipe[F, (KeyType, HttpResponse), (KeyType, String)]

NetgymHttpClient.toJsonResponses[F, KeyType] // returns Pipe[F, (KeyType, HttpResponse), (KeyType, Json)]

NetgymHttpClient.toObjectResponses[F, KeyType, T](implicit decoder: io.circe.Decoder[T]) // returns Pipe[F, (KeyType, HttpResponse), (KeyType, T)]
```
