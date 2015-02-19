import com.typesafe.config._

val conf = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()

name := """ls"""

version := conf.getString("app.version")

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
  "com.thoughtworks.xstream" % "xstream" % "1.4.7",  
  "org.hibernate" % "hibernate-entitymanager" % "4.3.7.Final",
  "org.hibernate" % "hibernate-spatial" % "4.3"
)
