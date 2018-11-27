package com.example.upload

import cats.effect.{ConcurrentEffect, Resource}
import com.example.upload.algebra.HttpClient
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

class BlazeClient[F[_] : ConcurrentEffect](ec: ExecutionContext) extends HttpClient[F] {
  override def client: Resource[F, Client[F]] = {
    BlazeClientBuilder[F](ec).resource
  }
}
