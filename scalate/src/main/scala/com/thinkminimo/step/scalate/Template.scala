package com.thinkminimo.step.scalate
import java.io.{File,StringWriter,PrintWriter}
import org.fusesource.scalate._
import scala.xml.Node

object Template { 
	
	private def makeTemplate(name: String): (StringWriter, DefaultRenderContext, Template) = { 
		val current = System.getProperty("user.dir");
	 	val templateFolder  = current+ "/templates/"
		val engine = new TemplateEngine
		val template = engine.load(templateFolder + "" + name)  
		val buffer = new StringWriter
		val context = new DefaultRenderContext(engine, new PrintWriter(buffer))
		return (buffer,context,template) 
		}
	
	def render(templateName: String) = { 
		val (buffer,context,template) =  makeTemplate(templateName)
		template.render(context) 
		buffer
		}
		
	def render(templateName: String, variables:(String,Any)*):java.io.StringWriter =  { 
		val (buffer,context,template) =  makeTemplate(templateName)
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