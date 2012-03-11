package org.scalatra.servlet;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

public class ServletContextWrapper implements ServletContext {
    private ServletContext servletContext;

    public ServletContextWrapper(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public String getContextPath() {
        return servletContext.getContextPath();
    }

    @Override
    public ServletContext getContext(String uripath) {
        return servletContext.getContext(uripath);
    }

    @Override
    public int getMajorVersion() {
        return servletContext.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return servletContext.getMinorVersion();
    }

    @Override
    public int getEffectiveMajorVersion() {
        return servletContext.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        return servletContext.getEffectiveMinorVersion();
    }

    @Override
    public String getMimeType(String file) {
        return servletContext.getMimeType(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return servletContext.getResourcePaths(path);
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return servletContext.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return servletContext.getResourceAsStream(path);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return servletContext.getRequestDispatcher(path);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return servletContext.getNamedDispatcher(name);
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return servletContext.getServlet(name);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return servletContext.getServlets();
    }

    @Override
    public Enumeration<String> getServletNames() {
        return servletContext.getServletNames();
    }

    @Override
    public void log(String msg) {
        servletContext.log(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        servletContext.log(exception, msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        servletContext.log(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        return servletContext.getRealPath(path);
    }

    @Override
    public String getServerInfo() {
        return servletContext.getServerInfo();
    }

    @Override
    public String getInitParameter(String name) {
        return servletContext.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return servletContext.getInitParameterNames();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return servletContext.setInitParameter(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return servletContext.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return servletContext.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object object) {
        servletContext.setAttribute(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        servletContext.removeAttribute(name);
    }

    @Override
    public String getServletContextName() {
        return servletContext.getServletContextName();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return servletContext.addServlet(servletName, className);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return servletContext.addServlet(servletName, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return servletContext.addServlet(servletName, servletClass);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return servletContext.createServlet(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return servletContext.getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return servletContext.getServletRegistrations();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return servletContext.addFilter(filterName, className);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return servletContext.addFilter(filterName, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return servletContext.addFilter(filterName, filterClass);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return servletContext.createFilter(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return servletContext.getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return servletContext.getFilterRegistrations();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return servletContext.getSessionCookieConfig();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        servletContext.setSessionTrackingModes(sessionTrackingModes);
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return servletContext.getDefaultSessionTrackingModes();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return servletContext.getEffectiveSessionTrackingModes();
    }

    @Override
    public void addListener(String className) {
        servletContext.addListener(className);
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        servletContext.addListener(t);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        servletContext.addListener(listenerClass);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return servletContext.createListener(clazz);
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return servletContext.getJspConfigDescriptor();
    }

    @Override
    public ClassLoader getClassLoader() {
        return servletContext.getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        servletContext.declareRoles(roleNames);
    }
}
