package com.example.client

import cats.Applicative
import cats.mtl.{ApplicativeAsk, DefaultApplicativeAsk}

object ApplicativeAsk {
  def constant[F[_] : Applicative, E](e: E): ApplicativeAsk[F, E] =
    new DefaultApplicativeAsk[F, E] {
      override val applicative: Applicative[F] = Applicative[F]
      override def ask: F[E] = applicative.pure(e)
    }
}


