/*
 *
 */
package org.mynotes.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public final class ErrorPage extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<link rel=\"stylesheet\" href=\"css/errorpage.css\">");
            writer.println("<title>Error</title>");
            writer.println("<body>");

            Throwable exception = (Throwable)request.getAttribute("javax.servlet.error.exception");
            Integer statusCode = (Integer)request.getAttribute("javax.servlet.error.status_code");
            String servletName = (String)request.getAttribute("javax.servlet.error.servlet_name");

            writer.println("Error " + response.getStatus());

            if (exception != null) {
                writer.println("<p>");
                writer.println(exception.getMessage());
                writer.println("</p>");
            }

            writer.println("</body>");
            writer.println("</html>");
        }
    }
    @Override
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
}



