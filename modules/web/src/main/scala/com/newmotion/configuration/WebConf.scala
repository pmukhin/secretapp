package com.newmotion.configuration

object WebConf extends ConfigSection {
  val section: String = "web"
}

case class WebConf(port: Int)
