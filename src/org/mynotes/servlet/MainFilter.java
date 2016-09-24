
package org.mynotes.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.annotation.WebFilter;

/* XXX filter does not apply to anything, uncomment following line and specify some rules */
//@WebFilter("/*")
public class MainFilter implements Filter
{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
//        ServletContext sc = request.getServletContext();

        if (request instanceof HttpServletRequest)
        {

            HttpServletRequest hrequest = (HttpServletRequest) request;

            String info = hrequest.getRequestURI();
            if (info.equals("/login.html"))
                throw new ServletException("Invalid request: Direct access denied");


//            sc.log("filter, \"" + info + "\"");
        }
        
        chain.doFilter(request, response);     

    }

    @Override
    public void destroy() {
    }
}
