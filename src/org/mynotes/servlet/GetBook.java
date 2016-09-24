/*
 *
 */
package org.mynotes.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mynotes.MyNotes;
import org.mynotes.User;
import org.mynotes.Book;
import org.mynotes.Note;

/*
 *
 */
public final class GetBook extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();

        User user = null;
        try {
            user = Home.getUserFromSession(session);
        } catch (Exception e) {
            throw new ServletException("Failed to get user from session");
        }
        if (user == null) {
            throw new ServletException("No user");
        }

        Long bookId = Home.getLongParameter(request, "bookid");
        if (bookId == null)
            throw new ServletException("No book id specified");

        Book book;
        try {
            book = Book.loadForUser(user, bookId);
        } catch (Exception e) {
            throw new ServletException("No access");
        }

        String field = request.getParameter("field");
        if (field == null)
            throw new ServletException("No book field specified");

        String value = null;
        switch (field) {
            case "name":
                value = book.name;
                break;
            case "description":
                value = book.description;
                break;
            case "tags":
                value = book.tags.concat();
                break;
            default:
                throw new ServletException("Unknown field");
        }

        if (value != null) {
            try (PrintWriter writer = response.getWriter()) {
                writer.print(value);
            }
        }
    }
}

