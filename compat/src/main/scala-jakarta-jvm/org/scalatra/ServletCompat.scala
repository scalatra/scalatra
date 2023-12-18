package org.scalatra

private[scalatra] object ServletCompat {
  type AsyncContext = jakarta.servlet.AsyncContext
  type AsyncEvent = jakarta.servlet.AsyncEvent
  type AsyncListener = jakarta.servlet.AsyncListener
  type DispatcherType = jakarta.servlet.DispatcherType
  type Filter = jakarta.servlet.Filter
  type FilterChain = jakarta.servlet.FilterChain
  type FilterConfig = jakarta.servlet.FilterConfig
  type MultipartConfigElement = jakarta.servlet.MultipartConfigElement
  type ReadListener = jakarta.servlet.ReadListener
  type Servlet = jakarta.servlet.Servlet
  type ServletConfig = jakarta.servlet.ServletConfig
  type ServletContext = jakarta.servlet.ServletContext
  type ServletContextEvent = jakarta.servlet.ServletContextEvent
  type ServletContextListener = jakarta.servlet.ServletContextListener
  type ServletInputStream = jakarta.servlet.ServletInputStream
  type ServletOutputStream = jakarta.servlet.ServletOutputStream
  type ServletRegistration = jakarta.servlet.ServletRegistration
  type ServletRequest = jakarta.servlet.ServletRequest
  type ServletResponse = jakarta.servlet.ServletResponse
  type WriteListener = jakarta.servlet.WriteListener

  object DispatcherType {
    def REQUEST = jakarta.servlet.DispatcherType.REQUEST
    def ASYNC = jakarta.servlet.DispatcherType.ASYNC
  }

  object http {
    type Cookie = jakarta.servlet.http.Cookie
    type HttpServlet = jakarta.servlet.http.HttpServlet
    type HttpServletRequest = jakarta.servlet.http.HttpServletRequest
    type HttpServletRequestWrapper = jakarta.servlet.http.HttpServletRequestWrapper
    type HttpServletResponse = jakarta.servlet.http.HttpServletResponse
    type HttpServletResponseWrapper = jakarta.servlet.http.HttpServletResponseWrapper
    type HttpSession = jakarta.servlet.http.HttpSession
    type HttpSessionAttributeListener = jakarta.servlet.http.HttpSessionAttributeListener
    type HttpSessionBindingEvent = jakarta.servlet.http.HttpSessionBindingEvent
    type Part = jakarta.servlet.http.Part
  }
}
