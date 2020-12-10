name := "SampleSparkRecommendation"

version := "0.1"

scalaVersion := "2.12.12"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.0.1",
  "org.apache.spark" %% "spark-sql" % "3.0.1",
  "org.apache.spark" %% "spark-mllib" % "3.0.1",
//  "com.nativelibs4java" % "scalacl" % "0.2",
//  "com.nativelibs4java" % "javacl-parent-jna" % "1.0.0-RC3", // force latest JavaCL version
//"com.nativelibs4java" %% "scalacl" % "0.3-SNAPSHOT"
)

//resolvers += "Sonatype OSS Snapshots Repository" at "https://oss.sonatype.org/content/groups/public/"
//resolvers += "NativeLibs4Java Repository" at "https://nativelibs4java.sourceforge.net/maven/"

//autoCompilerPlugins := true
//addCompilerPlugin("com.nativelibs4java" % "maven-javacl-plugin" % "1.0.0-RC3")
//
//fork := true
// Scalaxy/Reified snapshots are published on the Sonatype repository.
//resolvers += Resolver.sonatypeRepo("snapshots")
