package com.rossabaker.sbt.openpgp

import _root_.sbt._
import java.{util => ju}
import scala.collection.jcl.Conversions._
import org.apache.ivy.plugins.resolver._
import org.apache.ivy.plugins.signer._
import org.apache.ivy.plugins.signer.bouncycastle._

/**
 * Turns on Ivy's pgp mechanism.  It assumes:
 *
 * - that the system property pgp.password is set
 *
 * - That bouncy castle is on the boot classpath.  It must be available in
 *   the same class loader as Ivy.  This can be achieved with a custom
 *   sbt.boot.properties file.  See http://code.google.com/p/simple-build-tool/wiki/GeneralizedLauncher
 *
 * Failing either of these conditions, a warning is issued and publishing
 * continues successfully.
 */
trait SignWithOpenpgp extends BasicManagedProject {
  lazy val pgpName = "sbt-pgp"
  lazy val pgpSecring: Option[String] = system[String]("pgpSecring").get
  lazy val pgpKeyId: Option[String] = system[String]("pgp.keyId").get
  lazy val pgpPassword: Option[String] = system[String]("pgp.password").get

  lazy val pgpSignatureGenerator: Either[String, SignatureGenerator] = 
    pgpPassword match {
      case Some(password) =>
        try {
          val gen = new OpenPGPSignatureGenerator
          gen.setName(pgpName)
          pgpSecring foreach gen.setSecring
          pgpKeyId foreach gen.setKeyId
          gen.setPassword(password)
          Right(gen)
        }
        catch {
          case e: NoClassDefFoundError => 
            Left("openpgp not on classpath: "+e.getMessage)
        }
 
      case None => 
        Left("system property pgp.password not set")
    }

  override def ivySbt = {
    def setSigner(resolver: DependencyResolver): Unit = resolver match {
      case r: ChainResolver => 
        r.getResolvers foreach { 
          case child: DependencyResolver => setSigner(child)
        }
      case r: RepositoryResolver =>
        r.setSigner(pgpName)
    }

    val i = super.ivySbt
    pgpSignatureGenerator.right map { gen =>
      i.withIvy { ivy =>
        val settings = ivy.getSettings
        settings.addSignatureGenerator(gen)
        settings.getResolvers.toList foreach {
          case r: DependencyResolver => setSigner(r)
        }
      }
    }
    i
  }

  override def publishAction = 
    task {
      pgpSignatureGenerator.left map { reason =>
        log.warn("not signing artifacts: " + reason)
      }
      None
    } && super.publishAction

  private implicit def juCollection2Iterable[A](c: ju.Collection[A]): Iterable[A] = {
    val list = new ju.ArrayList[A](c.size)
    list.addAll(c)
    list
  }
}

