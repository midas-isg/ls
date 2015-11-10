import com.typesafe.config._

val conf = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()

name := """ls"""

version := conf.getString("app.version")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += "Hibernate Spatial" at "http://www.hibernatespatial.org/repository"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJpa.exclude(
  	"org.hibernate.javax.persistence", 
  	"hibernate-jpa-2.0-api"
  ),
  cache,
  javaWs,
  "javax.ws.rs" % "jsr311-api" % "0.11",
  "com.thoughtworks.xstream" % "xstream" % "1.4.7",  
  "org.hibernate" % "hibernate-entitymanager" % "4.3.7.Final",
  "org.hibernate" % "hibernate-spatial" % "4.3" ,
  "pl.matisoft" %% "swagger-play24" % "1.4",
  "org.easytesting" % "fest-assert" % "1.4" % Test
)
										  

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
