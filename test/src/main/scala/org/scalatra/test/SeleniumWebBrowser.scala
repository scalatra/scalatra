package org.scalatra.test

object SeleniumWebBrowser {
  trait Chrome extends org.scalatest.selenium.Chrome {

  }

  trait Firefox extends org.scalatest.selenium.Firefox {
    /**
     * remapping of a selenium/scalatest method since it conflicts with
     * a specs2 method
     */
    def nameQuery(elementName: String): NameQuery = {
      name(elementName)
    }
  }

  trait HtmlUnit extends org.scalatest.selenium.HtmlUnit {
    /**
     * remapping of a selenium/scalatest method since it conflicts with
     * a specs2 method
     */
    def nameQuery(elementName: String): NameQuery = {
      name(elementName)
    }
  }

  trait InternetExplorer extends org.scalatest.selenium.InternetExplorer {
    /**
     * remapping of a selenium/scalatest method since it conflicts with
     * a specs2 method
     */
    def nameQuery(elementName: String): NameQuery = {
      name(elementName)
    }
  }

  trait Safari extends org.scalatest.selenium.Safari {
    /**
     * remapping of a selenium/scalatest method since it conflicts with
     * a specs2 method
     */
    def nameQuery(elementName: String): NameQuery = {
      name(elementName)
    }
  }

  trait EmbeddedJettyContainerSelenium extends EmbeddedJettyContainer {
    override def port: Int = 8081
  }

  trait SeleniumTests extends EmbeddedJettyContainerSelenium with HttpComponentsClient { }
}