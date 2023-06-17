package org.scalatra

private[scalatra] object ServletCompat {
  type AsyncContext = javax.servlet.AsyncContext
  type AsyncEvent = javax.servlet.AsyncEvent
  type AsyncListener = javax.servlet.AsyncListener
  type DispatcherType = javax.servlet.DispatcherType
  type Filter = javax.servlet.Filter
  type FilterChain = javax.servlet.FilterChain
  type FilterConfig = javax.servlet.FilterConfig
  type MultipartConfigElement = javax.servlet.MultipartConfigElement
  type ReadListener = javax.servlet.ReadListener
  type Servlet = javax.servlet.Servlet
  type ServletConfig = javax.servlet.ServletConfig
  type ServletContext = javax.servlet.ServletContext
  type ServletContextEvent = javax.servlet.ServletContextEvent
  type ServletContextListener = javax.servlet.ServletContextListener
  type ServletInputStream = javax.servlet.ServletInputStream
  type ServletOutputStream = javax.servlet.ServletOutputStream
  type ServletRegistration = javax.servlet.ServletRegistration
  type ServletRequest = javax.servlet.ServletRequest
  type ServletResponse = javax.servlet.ServletResponse
  type WriteListener = javax.servlet.WriteListener

  object DispatcherType {
    def REQUEST = javax.servlet.DispatcherType.REQUEST
    def ASYNC = javax.servlet.DispatcherType.ASYNC
  }

  object http {
    type Cookie = javax.servlet.http.Cookie
    type HttpServlet = javax.servlet.http.HttpServlet
    type HttpServletRequest = javax.servlet.http.HttpServletRequest
    type HttpServletRequestWrapper = javax.servlet.http.HttpServletRequestWrapper
    type HttpServletResponse = javax.servlet.http.HttpServletResponse
    type HttpServletResponseWrapper = javax.servlet.http.HttpServletResponseWrapper
    type HttpSession = javax.servlet.http.HttpSession
    type HttpSessionAttributeListener = javax.servlet.http.HttpSessionAttributeListener
    type HttpSessionBindingEvent = javax.servlet.http.HttpSessionBindingEvent
    type Part = javax.servlet.http.Part
  }
}
