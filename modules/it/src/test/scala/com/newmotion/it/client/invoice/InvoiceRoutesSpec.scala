package com.newmotion.it.client.invoice

import cats.effect.specs2.CatsIO
import com.newmotion.it.IntegrationSpec
import org.specs2.mutable.Before

class InvoiceRoutesSpec extends IntegrationSpec with CatsIO with Before {
  sequential // we need it to run sequentially

  override def before: Unit = seed("tariffs", "clients", "sessions")

  "InvoiceRoutes" should {
    "build an invoice" in {
      get("/invoices/25/2017-04-12T13:20:50Z/2017-05-12T13:20:50Z.csv").map { r =>
        r.status must_=== 200
        r.bodyAsIn("01", `type` = "csv")
      }
    }
  }
}
