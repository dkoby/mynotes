/*
 *
 */
package org.mynotes.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import java.sql.SQLException;

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

//import javax.mail.internet.*;

import org.mynotes.MyNotes;
import org.mynotes.User;
import org.mynotes.exceptions.NoSuchUserException;
import org.mynotes.exceptions.DuplicateUserException;
import org.mynotes.exceptions.InvalidUserConfirmString;


/**
 *
 */
public final class Register extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());

        String userName = request.getParameter("user");
        String confirm  = request.getParameter("confirm");
        if (userName != null && confirm != null) {
            try {
                MyNotes.getInstance().confirmUser(userName, confirm);
            } catch (NoSuchUserException e) {
                throw new ServletException("No such user");
            } catch (InvalidUserConfirmString e) {
                throw new ServletException("Invalid confirm string");
            } catch (SQLException e) {
                throw new ServletException("DB error");
            }

            session.setAttribute("user", userName);
            response.sendRedirect("home");
            return;
        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<title>");
            {
                StringBuilder sb = new StringBuilder(msgcat.getString("APPLICATION_NAME"));
                sb.append(": ");
                sb.append(msgcat.getString("TITLE_LOGIN_REGISTER"));
                writer.println(sb.toString());
            }
            writer.println("</title>");
            writer.println("<link rel=\"stylesheet\" href=\"css/login.css\">");
            writer.println("</head>");

            writer.println("<body>");

            writer.println("<div class=\"overlay_dialog with_border\">");
            writer.println("        <form id=\"register\" action=\"register\" method=\"post\"></form>");

            writer.println("        <table>");

            writer.println("            <tr>");
            writer.print("                  <td>");
            writer.print(msgcat.getString("USER"));
            writer.print("</td><td>");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<input");
                sb.append(" id=\"register_name\"");
                sb.append(" form=\"register\"");
                sb.append(" type=\"text\"");
                sb.append(" name=\"user\"");
                sb.append(" autofocus");
                sb.append(" autocomplete=\"off\"");
                sb.append(" minlength=\"1\"");
                sb.append(" maxlength=\"20\"");
                sb.append(" required");
                sb.append(" tabindex=\"2\"");
                sb.append(">");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.print("                  <td>");
            writer.print(msgcat.getString("PASSWORD"));
            writer.print("</td><td>");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<input");
                sb.append(" id=\"register_pass\"");
                sb.append(" form=\"register\"");
                sb.append(" type=\"password\"");
                sb.append(" name=\"password\"");
                sb.append(" autocomplete=\"off\"");
                sb.append(" minlength=\"6\"");
                sb.append(" maxlength=\"40\"");
                sb.append(" required");
                sb.append(" tabindex=\"3\"");
                sb.append(">");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.print("                  <td>");
            writer.print(msgcat.getString("PASSWORD_REPEAT"));
            writer.print("</td><td>");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<input");
                sb.append(" id=\"register_pass_repeat\"");
                sb.append(" type=\"password\"");
                sb.append(" autocomplete=\"off\"");
                sb.append(" minlength=\"6\"");
                sb.append(" maxlength=\"40\"");
                sb.append(" required");
                sb.append(" tabindex=\"4\"");
                sb.append(">");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.print("                  <td>");
            writer.print(msgcat.getString("EMAIL"));
            writer.print("</td><td>");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<input");
                sb.append(" id=\"register_email\"");
                sb.append(" form=\"register\"");
                sb.append(" type=\"text\"");
                sb.append(" name=\"email\"");
                sb.append(" autofocus");
                sb.append(" autocomplete=\"off\"");
                sb.append(" required");
                sb.append(" tabindex=\"5\"");
                sb.append(">");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.print("                  <td colspan=\"2\">");
            writer.print("<span id=\"register_error\" class=\"login_error\">");
            String registerError = (String)session.getAttribute("registerError");
            if (registerError != null)
                writer.print(registerError);
            writer.print("</span>");
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.print("                <td colspan=\"2\" class=\"login_button_cell\">");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<button");
                sb.append(" class=\"button button_dark\"");
                sb.append(" onclick=\"register.process()\"");
                sb.append(" tabindex=\"6\"");
                sb.append(" onfocus=\"this.tabIndex=1\"");
                sb.append(" onblur=\"this.tabIndex=5\"");
                sb.append(">");
                sb.append(msgcat.getString("CONFIRM"));
                sb.append("</button>");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("        </table>");

            writer.println("</div>");

            writer.println("</body>");
            writer.println("<script src=\"js/generic.js\"></script>");
            writer.println("<script src=\"js/register.js\"></script>");
            writer.println("</html>");
        }

        session.removeAttribute("registerError");
    }
    /*
     *
     */
    @Override
    public void doPost(
            HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());

        HttpSession session = request.getSession();

        String userName = request.getParameter("user");
        String password = request.getParameter("password");
        String email    = request.getParameter("email");

        if (userName == null) {
            throw new ServletException("No user in request");
        }
        if (password == null) {
            throw new ServletException("No password in request");
        }
        if (email == null) {
            throw new ServletException("No email in request");
        }

        if (!userName.matches("^[a-zA-Z0-9_]{1,20}$")) {
            session.setAttribute("registerError", msgcat.getString("REGISTER_ERROR_NAME_INVAL"));
            response.sendRedirect("register");
            return;
        }
        /* TODO check password, email for correct input */

        /* TODO send email */

        try {
            MyNotes.getInstance().newUser(userName, password, email);
        } catch (SQLException e) {
            throw new ServletException("DB error");
        } catch (DuplicateUserException e) {
            session.setAttribute("registerError", msgcat.getString("REGISTER_ERROR_DUPLICATE_USER"));
            response.sendRedirect("register");
            return;
        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<title>");
            {
                StringBuilder sb = new StringBuilder(msgcat.getString("APPLICATION_NAME"));
                writer.println(sb.toString());
            }
            writer.println("</title>");
            writer.println("<link rel=\"stylesheet\" href=\"css/home.css\">");
            writer.println("</head>");

            writer.println("<body>");
            writer.println("<h3>");
            writer.println(msgcat.getString("REGISTER_CHECK_EMAIL"));
            writer.println(" (<strong>" + email + "</strong>).");
            writer.println("</h3>");
            writer.println("</body>");
            writer.println("</html>");
        }
    }
}

