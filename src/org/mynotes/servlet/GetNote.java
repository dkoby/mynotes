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
public final class GetNote extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();

//        /* XXX */
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {};

        User user = null;
        try {
            user = Home.getUserFromSession(session);
        } catch (Exception e) {
            throw new ServletException("Failed to get user from session");
        }
        if (user == null) {
            throw new ServletException("No user");
        }

        Long noteId = Home.getLongParameter(request, "noteid");
        if (noteId == null)
            throw new ServletException("Failed to get note ID");

        Note note;
        try {
            note = Note.loadById(noteId);
        } catch (Exception e) {
            throw new ServletException("Failed to load note");
        }

        Book book;
        try {
            book = Book.loadForUser(user, note.bookId);
        } catch (Exception e) {
            throw new ServletException("No access");
        }

//        if (!(book.access.equals("A") || note.authorId == user.id))
//            throw new ServletException("No access");

        String field = request.getParameter("field");
        if (field == null)
            throw new ServletException("No note field specified");

        String value = null;
        switch (field) {
            case "message":
                value = note.message;
                break;
            case "tags":
                value = note.tags.concat();
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

