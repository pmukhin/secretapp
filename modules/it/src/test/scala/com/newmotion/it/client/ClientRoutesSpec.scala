package com.newmotion.it.client

import cats.effect.specs2.CatsIO
import com.newmotion.it.IntegrationSpec
import org.specs2.specification.BeforeAll

class ClientRoutesSpec extends IntegrationSpec with CatsIO with BeforeAll {
  override def beforeAll: Unit = seed("01")

  "ClientRoutes" should sequential ^ {
    "serve a client if it exists" in {
      get("/clients/25").map { r =>
        r.status must_=== 200
        r.bodyAsIn("01")
      }
    }
    "not serve a client if it does not" in {
      get("/clients/250000").map { r =>
        r.status must_=== 404
      }
    }
    "not serve a client if it is deleted" in {
      get("/clients/2").map { r =>
        r.status must_=== 404
      }
    }
    "serve with right offset and limit" in {
      get("/clients?limit=10&offset=10").map { r =>
        r.status must_=== 200
        r.bodyAsIn("02")
      }
    }
    "serve with right default offset and limit" in {
      get("/clients").map { r =>
        r.status must_=== 200
        r.bodyAsIn("03")
      }
    }
    "serve right with deleted" in {
      get("/clients?limit=5&deleted=true").map { r =>
        r.status must_=== 200
        r.bodyAsIn("04")
      }
    }
    "serve 404 if entity is deleted" in {
      delete("/clients/2").map { r =>
        r.status must_=== 404
      }
    }
    "delete if entity is not deleted" in {
      delete("/clients/3").map { r =>
        r.status must_=== 200
      }
    }
    "create a client if valid age" in {
      post("/clients", asIn("05")).map { r =>
        r.status must_=== 200
        r.bodyAsIn("05", asJson = true, dropColumns = "id" :: Nil)
      }
    }
    "refuse if client is younger than 18" in {
      post("/clients", asIn("06")).map { r =>
        r.status must_=== 400
      }
    }
  }
}
