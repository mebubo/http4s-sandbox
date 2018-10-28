package com.example.client

import java.util.concurrent.Executors

import cats.effect._
import cats.implicits._
import fs2.hash
import org.http4s.Header
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object ClientSandbox extends IOApp with Http4sClientDsl[IO] {

  def program(clientIO: Client[IO]): IO[String] = {

    val blockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

    val path = java.nio.file.Paths.get("src/main/resources/logback.xml")

    val fileStream = fs2.io.file.readAll[IO](path, blockingExecutionContext, 4096)

    for {
      sha <- sha1(fileStream).compile.toVector
      val string = sha.head
      req <- PUT(uri("http://localhost:8080/status/201"), fileStream, Header("sha", string))
      r <- clientIO.expect[String](req)
      _ <- IO(println(r))
    } yield r
  }

  private def sha1(stream: fs2.Stream[IO, Byte]) = {
    stream.through(hash.sha1).map("%02X" format _).fold("")(_ + _)
  }

  def run(args: List[String]): IO[ExitCode] = {
    BlazeClientBuilder[IO](global).resource.use(program).as(ExitCode.Success)
  }

}