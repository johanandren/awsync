scalaVersion in Global := "2.11.4"

val versions = new {
  val akka = "2.3.7"
  val spray = "1.3.1"
  val scalaTest = "2.2.1"
}

scalacOptions in Global ++= Seq("-deprecation", "-feature", "-unchecked")

lazy val core = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % versions.akka,
      "io.spray" %% "spray-http" % versions.spray,
      "io.spray" %% "spray-client" % versions.spray,
      "org.scalatest" %% "scalatest" % versions.scalaTest % "test"
    )
  )

lazy val s3 = project
  .dependsOn(core)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "io.spray" %% "spray-client" % versions.spray,
      "io.spray" %% "spray-httpx" % versions.spray,
      "com.typesafe.akka" %% "akka-actor" % versions.akka,
      "org.scalatest" %% "scalatest" % versions.scalaTest % "it,test",
      "com.typesafe" % "config" % "1.2.1" % "test"
    ),
    parallelExecution in IntegrationTest := false
  )
