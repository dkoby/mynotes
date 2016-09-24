/*
 * XXX Users confirmation page. Should be disabled on production.
 */
package org.mynotes.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mynotes.MyNotes;
import org.mynotes.User;

/*
 *
 */
@WebServlet(urlPatterns = {"/confirm"})
public final class XXXConfirmUser extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        User currentUser = null;
        try {
            currentUser = Home.getUserFromSession(request.getSession());
        } catch (Exception e) {
            throw new ServletException("No access to this page");
        }

        if (currentUser != null) {
            if (!currentUser.name.equals("admin"))
                throw new ServletException("No access to this page");
        } else {
            Map<String, String> env = System.getenv();
            if (env.get("DEBUG") == null)
                throw new ServletException("No access to this page");
        }

        try (PrintWriter writer = response.getWriter()) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<title>Confirm</title>");
            writer.println("</head>");
            writer.println("<body>");

            Map<String, User> map = MyNotes.getInstance().getNewUsers();
            synchronized (map) {
                if (map.size() == 0) {
                    writer.println("No users to confirm");
                } else {
                    for (User user: map.values()) {
                        writer.println("<p>");
                        writer.print("<strong>" + user.name + " </strong>");
                        writer.println("<a href=\"register?user=" + user.name + "&confirm=" + user.confirm + "\">confirm</a>");
                        writer.println("</p>");
                    }
                }
            }

            writer.println("</body>");
            writer.println("</html>");
        }
    }
}

