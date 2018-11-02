package com.example.upload

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
    def upload(hs: data.Headers,
               e: data.Endpoint,
               s: fs2.Stream[F, Byte]): F[Unit]
  }

  trait Hashing[F[_]] {
    def hash: fs2.Pipe[F, Byte, Byte]
  }

}
