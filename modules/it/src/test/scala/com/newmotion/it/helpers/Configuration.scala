package com.newmotion.it.helpers

import com.newmotion.it.configuration.IntegrationSuiteConf
import pureconfig.ConfigSource

import scala.io.Source
import scala.util.Try

trait Configuration { self =>

  import pureconfig.generic.auto._

  private case class configReadError(errors: List[String])
      extends RuntimeException(s"following errors occurred:\n${errors.mkString("\n\t")}")

  protected val conf = ConfigSource.default
    .load[IntegrationSuiteConf]
    .fold(e => throw configReadError(e.toList.map(_.description)), identity)

  protected def loadResource(n: String): String = {
    val resource = getClass.getResource(n)
    Try(Source.fromURL(resource).getLines.mkString("\n")).toOption
      .getOrElse(throw new RuntimeException(s"resource: $n can not be opened"))
  }
}
