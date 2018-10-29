package com.example.client

import java.nio.file
import java.nio.file.Paths
import java.util.concurrent.Executors

import cats.data.ReaderT
import cats.effect._
import cats.implicits._
import cats.mtl.ApplicativeAsk
import fs2.{Pipe, Stream, hash}
import org.http4s.{Header, Headers, Request, Uri}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.Method.PUT
import org.http4s.Uri.uri

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
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

  def upload[F[_] : Sync : ContextShift : FileSystem : AppConfig.ConfigAsk](path: file.Path)(client: Client[F]): F[String] = {
    val fileStream = FileSystem[F].read(path)
    for {
      sha <- Hashing.sha1(fileStream).compile.toVector
      shaString = sha.head
      e <- Config.endpoint
      req = Request[F](PUT, e, body = fileStream, headers = Headers(Header("sha", shaString)))
      r <- client.expect[String](req)
    } yield r
  }

}

class UploadAndPrintResult[F[_] : Sync : ContextShift : ConcurrentEffect : Console : FileSystem : AppConfig.ConfigAsk](ec: ExecutionContext) {
  def run(path: file.Path): F[Unit] = for {
    out <- BlazeClientBuilder[F](ec).resource.use(HttpClient.upload[F](path))
    p <- Console[F].print(out)
  } yield ()
}

case class AppConfig(endpoint: Uri)

object AppConfig {
  type ConfigAsk[F[_]] = ApplicativeAsk[F, AppConfig]
  object ConfigAsk {
    def apply[F[_]](implicit F: ApplicativeAsk[F, AppConfig]): ConfigAsk[F] = F
  }
}

object Config {
  def endpoint[F[_] : AppConfig.ConfigAsk]: F[Uri] = AppConfig.ConfigAsk[F].reader(_.endpoint)
}

object ClientApp extends IOApp {

  import cats.mtl.implicits._

  def run(args: List[String]): IO[ExitCode] = {
    val path: file.Path = Paths.get("src/main/resources/logback.xml")
    implicit val blockingExecutionContext: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
    type App[A] = ReaderT[IO, AppConfig, A]
    implicit val c: Console[App] = new Console[App]
    implicit val fs: FileSystem[App] = new FileSystem[App](blockingExecutionContext)
    implicit val config: AppConfig = AppConfig(uri("https://httpbin.org/anything"))
    val value = new UploadAndPrintResult[App](global)
    val value1 = value.run(path)
    value1.run(config) as ExitCode.Success
  }

}