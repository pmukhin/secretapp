package com.newmotion.tvi

import fs2.Stream

object implicits {

  implicit class FOps[F[_], A](val fa: F[A]) extends AnyVal {

    def stream: Stream[F, A] =
      Stream.eval(fa)
  }
}
