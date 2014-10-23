name := """play23.gis"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += "Hibernate Spatial" at "http://www.hibernatespatial.org/repository"

resolvers += "aht" at "https://www.aht-group.com/nexus/content/repositories/public"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  //javaJdbc,
  javaJpa,
  //javaEbean,
  cache,
  javaWs,
  "org.hibernate" % "hibernate-entitymanager" % "4.2.2.Final",
  "com.google.inject" % "guice" % "3.0",
  "org.hibernate" % "hibernate-spatial" % "4.0-M1"
)
