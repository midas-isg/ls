name := """ls"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += "Hibernate Spatial" at "http://www.hibernatespatial.org/repository"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJpa.exclude(
  	"org.hibernate.javax.persistence", 
  	"hibernate-jpa-2.0-api"
  ),
  cache,
  javaWs,
  "org.hibernate" % "hibernate-entitymanager" % "4.3.6.Final",
  "org.hibernate" % "hibernate-spatial" % "4.3"
)
