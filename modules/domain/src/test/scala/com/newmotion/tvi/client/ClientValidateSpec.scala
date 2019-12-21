package com.newmotion.tvi.client

import java.time.LocalDate

import cats.effect.IO
import cats.effect.specs2.CatsIO
import org.mockito.specs2.Mockito
import org.specs2.mutable.Specification

class ClientValidateSpec extends Specification with CatsIO with Mockito {

  private val bodYounger18 =
    LocalDate.now().minusYears(17)

  private val bodJust18 =
    LocalDate.now().minusYears(18)

  private val sampleCommand =
    Client.CreateCommand("Test1", "Test2", bodYounger18)

  "Client.validateK" should {
    "raiseError if client is younger than 18yo" in {
      Client.validateK[IO].run(sampleCommand).attempt.map(_.isLeft must beTrue)
    }

    "do nothing if client is at least 18yo" in {
      Client
        .validateK[IO]
        .run(sampleCommand.copy(dob = bodJust18))
        .attempt
        .map(_.isRight must beTrue)
    }
  }
}
