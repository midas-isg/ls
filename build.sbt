import com.typesafe.config._

val conf = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()

name := """ls"""

version := conf.getString("app.version")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

resolvers += "Hibernate Spatial" at "http://www.hibernatespatial.org/repository"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaJpa, 
  cache,
  javaWs,
  "javax.ws.rs" % "jsr311-api" % "0.11",
  "com.thoughtworks.xstream" % "xstream" % "1.4.7",  
  "org.hibernate" % "hibernate-entitymanager" % "4.3.11.Final",
  "org.hibernate" % "hibernate-spatial" % "4.3" ,
  "pl.matisoft" %% "swagger-play24" % "1.4",
  "org.easytesting" % "fest-assert" % "1.4" % Test
)
										  

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)

// Needed as workaround for jpa persistence.xml bug   https://github.com/playframework/playframework/issues/4590
PlayKeys.externalizeResources := false