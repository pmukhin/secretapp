val Http4sVersion           = "0.20.8"
val CirceVersion            = "0.11.1"
val Specs2Version           = "4.7.1"
val LogbackVersion          = "1.2.3"
val RefinedVersion          = "0.9.10"
val ChimneyVersion          = "0.3.5"
val CatsVersion             = "2.0.0"
val MockitoScalaVersion     = "1.10.0"
val MysqlConnectorVersion   = "5.1.34"
val PureconfigVersion       = "0.12.1"
val DoobieVersion           = "0.8.+"
val CatsEffectSpecs2Version = "0.3.0"
val KindProjectorVersion    = "0.10.3"
val BetterMonadicVersion    = "0.3.0"
val MacroParadiseVersion    = "2.1.0"
val HamcrestVersion         = "2.1" // java lib

lazy val commonSettings = Seq(
  organization := "com.newmotion",
  scalaVersion := "2.12.8",
  version := "0.0.1",
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % KindProjectorVersion),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % BetterMonadicVersion),
  addCompilerPlugin(
    ("org.scalamacros" % "paradise" % MacroParadiseVersion).cross(CrossVersion.full)
  ),
  libraryDependencies ++= Seq(
    "org.specs2"     %% "specs2-core"                % Specs2Version % Test,
    "com.codecommit" %% "cats-effect-testing-specs2" % CatsEffectSpecs2Version % Test,
    "org.typelevel"  %% "cats-core"                  % CatsVersion,
    "org.typelevel"  %% "cats-effect"                % CatsVersion,
    "org.mockito"    %% "mockito-scala"              % MockitoScalaVersion % Test,
    "org.mockito"    %% "mockito-scala-specs2"       % MockitoScalaVersion % Test,
    "org.hamcrest"   % "hamcrest-library"            % HamcrestVersion % Test
  ),
  test in assembly := {},
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-language:postfixOps",
    "-feature",
    "-Ypartial-unification",
    "-Xfatal-warnings"
  )
)

lazy val root = (project in file("."))
  .aggregate(web, domain, doobieBackend)

lazy val domain = (project in file("modules/domain"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe"      %% "circe-generic" % CirceVersion,
      "org.typelevel" %% "cats-core"     % CatsVersion,
      "org.typelevel" %% "cats-effect"   % CatsVersion,
      "io.scalaland"  %% "chimney"       % ChimneyVersion
    )
  )

lazy val doobieBackend = (project in file("modules/doobie-be"))
  .settings(commonSettings)
  .dependsOn(domain)
  .settings(
    name := "doobie-be", // a.k.a. Doobie Back-end
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"         % DoobieVersion,
      "org.tpolecat" %% "doobie-quill"        % DoobieVersion,
      "mysql"        % "mysql-connector-java" % MysqlConnectorVersion
    )
  )

lazy val it = (project in file("modules/it"))
  .dependsOn(domain)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-blaze-client" % Http4sVersion,
      "com.github.pureconfig" %% "pureconfig"          % PureconfigVersion,
      "org.tpolecat"          %% "doobie-core"         % DoobieVersion,
      "org.tpolecat"          %% "doobie-quill"        % DoobieVersion,
      "mysql"                 % "mysql-connector-java" % MysqlConnectorVersion,
      "io.circe"              %% "circe-core"          % CirceVersion,
      "io.circe"              %% "circe-parser"        % CirceVersion
    )
  )

lazy val csv2s = (project in file("modules/csv2s"))
  .settings(commonSettings)

lazy val web = (project in file("modules/web"))
  .settings(commonSettings)
  .dependsOn(domain, doobieBackend, csv2s)
  .settings(
    mainClass in assembly := Some("com.newmotion.Main"),
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig"             % PureconfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureconfigVersion,
      "org.http4s"            %% "http4s-blaze-server"    % Http4sVersion,
      "org.http4s"            %% "http4s-blaze-client"    % Http4sVersion,
      "org.http4s"            %% "http4s-circe"           % Http4sVersion,
      "org.http4s"            %% "http4s-dsl"             % Http4sVersion,
      "io.circe"              %% "circe-generic"          % CirceVersion,
      "io.circe"              %% "circe-refined"          % CirceVersion,
      "ch.qos.logback"        % "logback-classic"         % LogbackVersion,
      "eu.timepit"            %% "refined"                % RefinedVersion,
      "io.scalaland"          %% "chimney"                % ChimneyVersion
    )
  )
