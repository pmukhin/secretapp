package com.newmotion.configuration

object DatabaseConf extends ConfigSection {
  val section = "database"
}

case class DatabaseConf(
  host: String,
  port: Int,
  name: String,
  username: String,
  password: String,
  threads: Int
) extends Product { lazy val url = s"jdbc:mysql://$host:$port/$name" }
