package com.seitenbau.micgwaf.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.seitenbau.micgwaf.component.Component;
import com.seitenbau.micgwaf.config.ApplicationBase;

public class WebappFilter implements Filter
{
  public static final String APPLICATION_CLASS_NAME_INIT_PARAM = "applicationClassName";
  
  public ApplicationBase application;
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException
  {
    String applicationClassName = filterConfig.getInitParameter(APPLICATION_CLASS_NAME_INIT_PARAM);
    try
    {
      application = (ApplicationBase) Class.forName(applicationClassName).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
    {
      throw new ServletException(e);
    }
  }

  @Override
  public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain) 
      throws IOException, ServletException
  {
    PrintWriter writer = response.getWriter();
    Component homePage = application.getHomePage();
    homePage.processRequest((HttpServletRequest) request);
    homePage.render(writer);
    writer.close();
  }

  @Override
  public void destroy()
  {
  }

}