resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.url(
  "bintray-sbilinski",
  url("http://dl.bintray.com/sbilinski/maven")
)(Resolver.ivyStylePatterns)

addSbtPlugin("com.github.gseitz" % "sbt-release"            % "1.0.9")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"                % "1.1.1")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"           % "2.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"                % "1.0.0")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"           % "0.14.7")
addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker"             % "1.5.0")
addSbtPlugin("com.mintbeans"     % "sbt-ecr"                % "0.10.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"           % "2.0.4")
addSbtPlugin("com.typesafe"      % "sbt-mima-plugin"        % "0.6.1")
