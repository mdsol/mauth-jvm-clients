resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.url(
  "bintray-sbilinski",
  url("https://dl.bintray.com/sbilinski/maven")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.github.gseitz" % "sbt-release"            % "1.0.12")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"                % "2.0.0")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"           % "3.7")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"                % "1.0.0")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"           % "0.14.10")
addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker"             % "1.5.0")
addSbtPlugin("com.mintbeans"     % "sbt-ecr"                % "0.15.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"           % "2.0.4")
