package com.newmotion

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.newmotion.tvi.TVIApp

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    TVIApp.stream[IO].compile.drain.as(ExitCode.Success)
}
