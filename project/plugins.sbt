resolvers ++= Seq(
  "Maven Central"          at "http://repo1.maven.org/maven2/",
  Classpaths.typesafeReleases,
  "sbt-idea-repo"          at "http://mpeltonen.github.com/maven/",
  "gseitz@github"          at "http://gseitz.github.com/maven/",
  "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo"
)

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.10")


