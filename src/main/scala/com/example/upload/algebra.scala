package com.example.upload

import cats.effect.Resource
import org.http4s.client.Client
import org.http4s.{Headers, Status, Uri}

object algebra {

  trait DataSource[F[_]] {
    def write(p: data.FilePath): F[Unit]
  }

  trait FileSystem[F[_]] {
    def createTmpFile: F[data.FilePath]
    def delete(p: data.FilePath): F[Unit]
    def read(p: data.FilePath): fs2.Stream[F, Byte]
  }

  trait Http[F[_]] {
    def upload(hs: Headers,
               e: Uri,
               s: fs2.Stream[F, Byte])(c: Client[F]): F[Status]
  }

  trait HttpClient[F[_]] {
    def client: Resource[F, Client[F]]
  }

  trait Hashing[F[_]] {
    def hash: fs2.Pipe[F, Byte, Byte]
  }

}
