name := "sierra"

version := "0.1.0"

scalaVersion := "2.11.8"

organization := "org.naoh"

resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4",
  "org.scala-lang" % "scala-reflect" % "2.11.8",
  "redis.clients" % "jedis" % "2.8.1",
  "org.scodec" %% "scodec-core" % "1.10.2",
  "org.scodec" %% "scodec-bits" % "1.1.0",
  "org.scalatest" %% "scalatest"  % "2.2.5" % "test"
)

publishTo := Some(Resolver.file("sierra",file("./"))(Patterns(true, Resolver.mavenStyleBasePattern)))
