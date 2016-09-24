/*
 *
 */
package org.mynotes.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.mynotes.MyNotes;
import org.mynotes.User;
import org.mynotes.exceptions.NoSuchUserException;
import org.mynotes.exceptions.DuplicateUserException;

/**
 *
 */
public final class Login extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();

        if (request.getParameter("logout") != null) {
            session.removeAttribute("user");
        } else {
            String userName = (String)session.getAttribute("user");
            if (userName != null)
            {
                response.sendRedirect("home");
                return;
            }
        }

        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());

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
            writer.println("        <form id=\"login\" action=\"login\" method=\"post\"></form>");
            writer.println("        <table>");

            writer.println("            <tr>");
            writer.print("                  <td>");
            writer.print(msgcat.getString("USER"));
            writer.print("</td><td>");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<input");
                sb.append(" id=\"login_name\"");
                sb.append(" form=\"login\"");
                sb.append(" type=\"text\"");
                sb.append(" name=\"user\"");
                sb.append(" autofocus");
                sb.append(" autocomplete=\"off\"");
                sb.append(" minlength=\"1\"");
                sb.append(" maxlength=\"20\"");
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
                sb.append(" id=\"login_pass\"");
                sb.append(" form=\"login\"");
                sb.append(" type=\"password\"");
                sb.append(" name=\"password\"");
                sb.append(" autocomplete=\"off\"");
                sb.append(" minlength=\"6\"");
                sb.append(" maxlength=\"40\"");
                sb.append(" tabindex=\"3\"");
                sb.append(">");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.println("                <td>");
            {
                StringBuilder sb = new StringBuilder();

                sb.append("<input ");
                sb.append(" type=\"checkbox\"");
                sb.append(" id=\"login_remcb\"");
                sb.append(" form=\"login\"");
                sb.append(" name=\"remember\"");
                sb.append(" tabindex=\"5\"");
                sb.append(" checked");
                sb.append(">");
                writer.print(sb.toString());
            }
            writer.print("<span class=\"login_remember\" onclick=\"login.clickRemember()\">");
            writer.print(msgcat.getString("REMEMBER"));
            writer.println("</span>");
            writer.println("                </td>");

            writer.print("                <td class=\"login_reg_td\">");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<span ");
                sb.append(" class=\"login_register\"");
                sb.append(" onclick=\"login.register()\"");
                sb.append(" onkeypress=\"login.register()\"");
                sb.append(" tabindex=\"6\"");
                sb.append(" onfocus=\"this.tabIndex=1\"");
                sb.append(" onblur=\"this.tabIndex=5\"");
                sb.append(">");
                sb.append(msgcat.getString("REGISTER"));
                sb.append("</span>");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.print("                  <td colspan=\"2\">");
            writer.print("<span id=\"login_error\" class=\"login_error\">");
            String authError = (String)session.getAttribute("authError");
            if (authError != null)
                writer.print(authError);
            writer.print("</span>");
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("            <tr>");
            writer.print("                <td colspan=\"2\" class=\"login_button_cell\">");
            {
                StringBuilder sb = new StringBuilder();
                sb.append("<button");
                sb.append(" class=\"button button_dark\"");
                sb.append(" form=\"login\"");
                sb.append(" onclick=\"login.enter()\"");
                sb.append(" tabindex=\"4\"");
                sb.append(">");
                sb.append(msgcat.getString("LOGIN"));
                sb.append("</button>");
                writer.print(sb.toString());
            }
            writer.println("</td>");
            writer.println("            </tr>");

            writer.println("        </table>");
            writer.println("</div>");

            writer.println("</body>");
            writer.println("<script src=\"js/login.js\"></script>");
            writer.println("</html>");
        }

        session.removeAttribute("authError");
    }
    /*
     *
     */
    @Override
    public void doPost(
            HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        session.removeAttribute("user");
        session.removeAttribute("authError");

        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());

        String userName  = request.getParameter("user");
        String password  = request.getParameter("password");
        boolean remember = request.getParameter("remember") == null ? false : true;

        if (userName == null || password == null)
        {
            log("(E) User/password is null");
            request.getRequestDispatcher("WEB-INF/deferror.html").forward(request, response);
            return;
        }

        log("Login user: \"" + userName + "\", remember: \"" + remember + "\"");

        /* TODO auth limit counter */

        User user;
        try {
            user = User.loadByName(userName);
        } catch (SQLException e) {
            throw new ServletException("DB error");
        } catch (NoSuchUserException e) {
            session.setAttribute("authError", msgcat.getString("LOGIN_ERROR_NO_SUCH_USER"));
            response.sendRedirect("login");
            return;
        }
        if (!user.password.equals(password)) {
            session.setAttribute("authError", msgcat.getString("LOGIN_ERROR_INVALID_PASSWORD"));
            response.sendRedirect("login");
            return;
        }

        if (remember)
            session.setMaxInactiveInterval(30 * 24 * 60 * 60); /* 30 days */
        else
            session.setMaxInactiveInterval(10 * 60); /* 10 minutes */

        session.setAttribute("user", user.name);
        response.sendRedirect("home");
    }
}
//        Cookie cookie = new Cookie("USERTOKEN", "asdf");
//        cookie.setSecure(true);
//        response.addCookie(cookie);


//            request.getRequestDispatcher("WEB-INF/login.html").include(request, response);

