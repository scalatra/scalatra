package com.thinkminimo.step.scalate
import java.io.{File,StringWriter,PrintWriter}
import org.fusesource.scalate._
import scala.xml.Node
import javax.servlet.ServletContext
import servlet.ServletResourceLoader

object Template { 
	
	private def makeTemplate(name: String, servletContext: ServletContext): (StringWriter, DefaultRenderContext, Template) = {
		val engine = new TemplateEngine
    engine.resourceLoader = new ServletResourceLoader(servletContext)
		val template = engine.load(name)  
		val buffer = new StringWriter
		val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
		return (buffer,context,template) 
		}
	
	def render(templateName: String)(implicit servletContext: ServletContext) = {
		val (buffer,context,template) =  makeTemplate(templateName, servletContext)
		template.render(context) 
		buffer
		}
		
	def render(templateName: String, variables:(String,Any)*)(implicit servletContext: ServletContext) :java.io.StringWriter =  {
		val (buffer,context,template) = makeTemplate(templateName, servletContext)
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