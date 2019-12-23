package com.newmotion.it.session

import cats.effect.specs2.CatsIO
import com.newmotion.it.IntegrationSpec
import org.specs2.specification.BeforeAll

class SessionRoutesSpec extends IntegrationSpec with CatsIO with BeforeAll {
  override def beforeAll(): Unit = seed("01")

  "SessionRoutes" should sequential ^ {
    "serve session if exists" in {
      get("/sessions/24").map { r =>
        r.status must_=== 200
        r.bodyAsIn("01")
      }
    }
    "serve 404 if does not" in {
      get("/sessions/520").map { r =>
        r.status must_=== 404
      }
    }
    "create session if request is valid" in {
      post("/sessions", asIn("02")).map { r =>
        r.status must_=== 200
      }
    }
    "not create session if start > end" in {
      post("/sessions", asIn("03")).map { r =>
        r.status must_=== 400
      }
    }
    "create session if start == end" in {
      post("/sessions", asIn("04")).map { r =>
        r.status must_=== 400
      }
    }
  }
}
