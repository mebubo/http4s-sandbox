package com.example.client

import java.nio.file
import java.nio.file.Paths
import java.util.concurrent.Executors

import cats.Monad
import cats.effect._
import cats.implicits._
import fs2.{Pipe, Stream, hash}
import org.http4s.{Header, Headers, Request}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.Method.PUT
import org.http4s.Uri.uri

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object Hashing {
  def sha1[F[_]]: Pipe[F, Byte, String] = { stream =>
    stream.through(hash.sha1).map("%02X" format _).fold("")(_ + _)
  }
}

class FileSystem[F[_] : Sync : ContextShift](ec: ExecutionContext) {
  def read(path: file.Path): Stream[F, Byte] = {
    fs2.io.file.readAll[F](path, ec, 4096)
  }
}

object FileSystem {
  def apply[F[_]](implicit F: FileSystem[F]): FileSystem[F] = F
}

class Console[F[_] : Sync] {
  def print(s: String): F[Unit] = Sync[F].delay {
    println(s)
  }
}

object Console {
  def apply[F[_]](implicit F: Console[F]): Console[F] = F
}

object HttpClient {

  def upload[F[_] : Sync : ContextShift : FileSystem](path: file.Path)(client: Client[F]): F[String] = {
    val fileStream = FileSystem[F].read(path)
    for {
      sha <- Hashing.sha1(fileStream).compile.toVector
      shaString = sha.head
      req = Request[F](PUT, uri("https://httpbin.org/anything"), body = fileStream, headers = Headers(Header("sha", shaString)))
      r <- client.expect[String](req)
    } yield r
  }

}

class UploadAndPrintResult[F[_] : Monad : ContextShift : ConcurrentEffect : Console : FileSystem](ec: ExecutionContext) {
  def run(path: file.Path): F[Unit] = for {
    out <- BlazeClientBuilder[F](ec).resource.use(HttpClient.upload[F](path))
    p <- Console[F].print(out)
  } yield ()
}

object ClientApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val path: file.Path = Paths.get("src/main/resources/logback.xml")
    val blockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
    implicit val c: Console[IO] = new Console[IO]
    implicit val fs: FileSystem[IO] = new FileSystem[IO](blockingExecutionContext)
    new UploadAndPrintResult[IO](global).run(path) as ExitCode.Success
  }

}