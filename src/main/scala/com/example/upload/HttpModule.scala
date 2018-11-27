package com.example.upload

import com.example.upload.algebra.{Http, HttpClient}
import org.http4s.client.Client
import org.http4s.{Headers, Request, Status, Uri}
import org.http4s.Method.PUT

class HttpModule[F[_]](H: HttpClient[F]) extends Http[F] {
  override def upload(hs: Headers, e: Uri, s: fs2.Stream[F, Byte])(c: Client[F]): F[Status] = {
    val req = Request[F](PUT, e, body = s, headers = hs)
    c.status(req)
  }
}
