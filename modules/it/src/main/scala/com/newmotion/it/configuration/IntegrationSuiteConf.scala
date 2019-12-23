package com.newmotion.it.configuration

case class DatabaseConf(
  jdbcUrl: String,
  username: String,
  password: String
)

case class IntegrationSuiteConf(
  database: DatabaseConf,
  baseUri: String
)
