name := "GeneticAlgoritm_Spark"

version := "1.0"

scalaVersion := "2.10.5"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "spray repo" at "http://repo.spray.io"

val sparkVersion = "1.6.0"

libraryDependencies ++= Seq(
  "org.apache.spark"  %% "spark-core"    % sparkVersion   % "provided",
  "org.apache.spark"  %% "spark-graphx"  % sparkVersion   % "provided",
  "org.apache.spark"  %% "spark-mllib"   % sparkVersion   % "provided",
  "org.apache.spark"  %% "spark-sql"     % sparkVersion   % "provided",
  "org.scalatest"     % "scalatest_2.10" % "2.0" % "test"
)
