/*
 *
 */
package org.mynotes.servlet;

import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.mynotes.MyNotes;
import org.mynotes.User;
import org.mynotes.Note;
import org.mynotes.Book;

/**
 *
 */
public class GetImage extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        String requestImage = request.getPathInfo();
        File file = null;


        if (requestImage.startsWith("/book/")) {
            String fileName = requestImage.substring(6) /* XXX */;
            file = new File(MyNotes.getInstance().bookIconPath, fileName); 
        } else if (requestImage.startsWith("/note/")) {
            String fileName = requestImage.substring(6) /* XXX */;

            file = new File(MyNotes.getInstance().noteImagePath, fileName); 

            /* 
             * Check user access to note image.
             *
             * XXX Too expensive for processing.
             *
             */
            if (true) {
                Matcher matcher = Pattern.compile("image([\\d]+)").matcher(fileName);
                Long id;
                if (matcher.find()) {
                    try {
                        id = new Long(matcher.group(1));
                    } catch (NumberFormatException e) {
                        throw new ServletException("Failed to get note id from image file name");
                    }
                } else {
                    throw new ServletException("Failed to get note id from image file name");
                }

                Note note;
                try {
                    note = Note.loadById(id);
                } catch (Exception e) {
                    throw new ServletException(e);
                }

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

                if (note.authorId != user.id) {
                    Book book;
                    try {
                        book = Book.loadForUser(user, note.bookId);
                    } catch (Exception e) {
                        request.getRequestDispatcher("/img/note_no_access.png").forward(request, response);
                        return;
                    }
                }
            }
        } else {
            throw new ServletException("Unknown image");
        }

        if (!file.exists())
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!isModifiedSince(file, request, response)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setContentType("image/png");
        try (OutputStream outputStream = response.getOutputStream()) {
            Files.copy(FileSystems.getDefault().getPath(file.toString()), outputStream);
            return;
        }
    }

    /*
     *
     */
    private boolean isModifiedSince(File file, HttpServletRequest request, HttpServletResponse response) {
        long ifModifiedSince  = request.getDateHeader("If-Modified-Since");
        long fileLastModified = file.lastModified();

        response.setDateHeader("Last-Modified", fileLastModified);
        if (ifModifiedSince >= 0 && (fileLastModified <= ifModifiedSince))
            return false;
        return true;
    }
}

