package slamdata

import sbt._, Keys._
import bintray.BintrayKeys._
import com.typesafe.sbt.SbtPgp.autoImportImpl.PgpKeys
import sbtrelease.ReleasePlugin.autoImport.{ releaseCrossBuild, releasePublishArtifactsAction }

class Publish {
  lazy val checkHeaders = taskKey[Unit]("Fail the build if createHeaders is not up-to-date")

  lazy val publishAsOSSProject = settingKey[Boolean](
    "Determines if project should be released publicly both to bintray and maven or only to a private bintray repository")

  lazy val synchronizeWithMavenCentral = taskKey[Unit]("Synchronize artifacts published on bintray with maven central")
  lazy val closeMavenCentralStaging = taskKey[Unit]("Close the sonatype staging repository")

  lazy val performSonatypeSync = settingKey[Boolean]("If true, then project will be sync'd from maven-public to Maven Central, but only if publishAsOSSProject is also true")

  lazy val commonPublishSettings = Seq(
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    publishAsOSSProject := true,
    performSonatypeSync := true,
    bintrayRepository := { if (publishAsOSSProject.value) "maven-public" else "maven-private" },
    synchronizeWithMavenCentral := Def.taskDyn {
      if (publishAsOSSProject.value && performSonatypeSync.value && !sbtPlugin.value) {
        Def.task(bintraySyncSonatypeStaging.value)
      } else {
        Def.task(())
      }
    }.value,
    // this is a little weird, because we overwrite it every time. whatever
    closeMavenCentralStaging in Global := Def.taskDyn {
      if (publishAsOSSProject.value && performSonatypeSync.value && !sbtPlugin.value) {
        Def.task(bintraySyncMavenCentral.value)
      } else {
        Def.task(())
      }
    }.value,
    bintraySyncMavenCentralRetries := {
      import scala.concurrent.duration._
      Seq(5.seconds, 1.minute, 5.minutes)
    },
    publishMavenStyle := true,
    bintrayOrganization := Some("slamdata-inc"),
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    releaseCrossBuild := true,
    autoAPIMappings := true,
    developers := List(
      Developer(
        id = "slamdata",
        name = "SlamData Inc.",
        email = "contact@slamdata.com",
        url = new URL("http://slamdata.com")
      )
    ),
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    PgpKeys.pgpPublicRing in Global := file("./project/local.pubring.pgp"),
    PgpKeys.pgpSecretRing in Global := file("./project/local.secring.pgp"),
    bintrayCredentialsFile := file("./local.credentials.bintray"),
    credentials ++= Seq(file("./local.credentials.sonatype")).filter(_.exists).map(Credentials(_))
  )

  lazy val noPublishSettings = Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

}

object Publish extends Publish
