resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.url("bintray-sbilinski", url("http://dl.bintray.com/sbilinski/maven"))(Resolver.ivyStylePatterns)

conflictManager := ConflictManager.strict

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")
addSbtPlugin("com.mintbeans" % "sbt-ecr" % "0.8.0")

dependencyOverrides ++= Seq(
  "com.jcraft" % "jsch" % "0.1.53",
  "commons-logging" % "commons-logging" % "1.2",
  "org.apache.commons" % "commons-lang3" % "3.5",
  "org.apache.httpcomponents" % "httpclient" % "4.5.5",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
  "org.slf4j" % "slf4j-api" % "1.7.25"
)
