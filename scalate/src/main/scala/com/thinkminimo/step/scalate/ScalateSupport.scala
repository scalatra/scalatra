package com.thinkminimo.step.scalate
import java.io.{File,StringWriter,PrintWriter}
import org.fusesource.scalate._
import scala.xml.Node
import javax.servlet.ServletContext
import servlet.ServletResourceLoader

trait ScalateSupport {
  self: { def servletContext: ServletContext } =>

  // Laziness lets the servlet context initialize itself first.
  private lazy val templateEngine = {
    val result = new TemplateEngine
    result.resourceLoader = new ServletResourceLoader(servletContext)
    result
  }
	
	private def makeTemplate(name: String): (StringWriter, DefaultRenderContext, Template) = {
		val template = templateEngine.load(name)  
		val buffer = new StringWriter
		val context = new DefaultRenderContext(templateEngine, new PrintWriter(buffer))
		return (buffer,context,template) 
		}
	
	def renderTemplate(templateName: String) = {
		val (buffer,context,template) =  makeTemplate(templateName)
		template.render(context) 
		buffer
		}
		
	def renderTemplate(templateName: String, variables:(String,Any)*) :java.io.StringWriter =  {
		val (buffer,context,template) = makeTemplate(templateName)
		for (variable <- variables) { 
			val (key, value) = variable
			context.attributes(key) = value
			}
		template.render(context)
		buffer
		}
	
	def renderToXHML(title: String, content: Seq[Node]) = { 
		<html> 
		  <head> 
			<title>{title}</title>	
		  </head>
		<body>
			{content}
		</body>
		</html>
		}
	
	}