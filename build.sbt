ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name             := "vafs",
    idePackagePrefix := Some("ru.beeline.vafs")
  )

lazy val akkaVersion         = "2.7.0"
lazy val akkaStreamVersion   = "4.0.0"
lazy val cassandraVersion    = "1.1.0"
lazy val alpakkaSlickVersion = "5.0.0"
lazy val PostgresVersion     = "42.5.0"
lazy val logbackVersion      = "1.4.4"
lazy val ZioVersion          = "1.0.4"
lazy val CirceVersion        = "0.14.2"
lazy val LiquibaseVersion    = "3.4.2"
lazy val ZIOHttpVersion      = "1.0.0.0-RC27"
lazy val QuillVersion        = "3.12.0"
lazy val Http4sVersion       = "0.21.7"
lazy val ZIOKafkaVersion     = "0.14.0"

libraryDependencies ++= Seq(
  "dev.zio"            %% "zio"                                 % ZioVersion,
  "dev.zio"            %% "zio-macros"                          % ZioVersion,
  "dev.zio"            %% "zio-config"                          % ZioVersion,
  "dev.zio"            %% "zio-config-magnolia"                 % ZioVersion,
  "dev.zio"            %% "zio-config-typesafe"                 % ZioVersion,
  "dev.zio"            %% "zio-kafka"                           % ZIOKafkaVersion,

  "io.getquill"        %% "quill-jdbc-zio"                      % QuillVersion,

  "io.d11"             %% "zhttp"                               % ZIOHttpVersion,

  "org.liquibase"       % "liquibase-core"                      % LiquibaseVersion,

  "org.postgresql"      % "postgresql"                          % PostgresVersion,

  "io.circe"           %% "circe-parser"                        % CirceVersion,
  "io.circe"           %% "circe-generic"                       % CirceVersion,

  "com.typesafe.akka"  %% "akka-persistence-cassandra"          % cassandraVersion,
  "com.typesafe.akka"  %% "akka-persistence-cassandra-launcher" % cassandraVersion,
  "com.typesafe.akka"  %% "akka-actor-typed"                    % akkaVersion,
  "com.typesafe.akka"  %% "akka-persistence-typed"              % akkaVersion,
  "com.typesafe.akka"  %% "akka-coordination"                   % akkaVersion,
  "com.typesafe.akka"  %% "akka-cluster"                        % akkaVersion,
  "com.typesafe.akka"  %% "akka-cluster-tools"                  % akkaVersion,
  "com.typesafe.akka"  %% "akka-stream-typed"                   % akkaVersion,
  "com.typesafe.akka"  %% "akka-stream-kafka"                   % akkaStreamVersion,

  "com.lightbend.akka" %% "akka-stream-alpakka-slick"           % alpakkaSlickVersion,

  "ch.qos.logback"      % "logback-classic"                     % logbackVersion
)

scalacOptions += "-Ymacro-annotations"
