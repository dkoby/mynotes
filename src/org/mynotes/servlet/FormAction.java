/*
 *
 */
package org.mynotes.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.File;

import java.util.Date;
import java.util.logging.Level;
import java.util.ArrayList;

import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.MultipartConfig;
//import javax.servlet.annotation.WebServlet;
//import javax.servlet.annotation.ServletSecurity;
//import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
//import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
//import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import javax.imageio.ImageIO;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Dimension;

//import javax.mail.internet.*;

import org.mynotes.MyNotes;
import org.mynotes.Limit;
import org.mynotes.User;
import org.mynotes.Book;
import org.mynotes.Note;
import org.mynotes.Tags;
import org.mynotes.exceptions.NoSuchUserException;

/**
 *
 */
@MultipartConfig
public final class FormAction extends HttpServlet {
    private static final Level LOGLEVEL_INFO  = Level.INFO;
    private static final Level LOGLEVEL_ERROR = Level.SEVERE;

    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        doPost(request, response);
    }
    /*
     *
     */
    @Override
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();

        request.setCharacterEncoding("UTF-8");

        String formType = request.getParameter("type");
        if (formType == null)
            throw new ServletException("No form type");

        String error = null;
        try {
            switch (formType) {
                case "makebook":
                    error = makeBook(request, response);
                    break;
                case "deletebook":
                    error = deleteBook(request, response);
                    break;
                case "deletenote":
                    error = deleteNote(request, response);
                    break;
                case "makenote":
                    error = makeNote(request, response);
                    break;
                default:
                    throw new ServletException("Unknown form type");
            }
        } catch (ServletException e) {
            MyNotes.log(LOGLEVEL_ERROR, "Formaction exception, " + e);
            throw e;
        }

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(error != null ? error : "success");
        }
    }

    /*
     *
     */
    private String makeBook(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());
        HttpSession session = request.getSession();

        User user = null;
        try {
            user = Home.getUserFromSession(request.getSession());
        } catch (Exception e) {
            throw new ServletException("Failed to get user from session");
        }
        if (user == null)
            throw new ServletException("No user");

        String bookName        = request.getParameter("name");
        String bookDescription = request.getParameter("description");
        String bookTags        = request.getParameter("tags");

        if (bookName == null)
            throw new ServletException("No book name");
        if (bookDescription == null)
            throw new ServletException("No book description");
        if (bookTags == null)
            throw new ServletException("No book tags");

        Part iconFilePart = request.getPart("icon");
        if (iconFilePart == null)
            throw new ServletException("No book icon");

//    Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
//    String fileName = filePart.getSubmittedFileName();
//    InputStream fileContent = filePart.getInputStream();
//    // ... (do your job here)        

        MyNotes.log(LOGLEVEL_INFO, "Create/store book \"" + bookName + "\" for user \"" + user.name + "\"");

        if (bookName.length() == 0) {
            return msgcat.getString("DIALOG_CREATE_BOOK_ERROR_EMPTY_BOOK_NAME");
        }

        if (bookName.length() > Limit.BOOK_NAME_LENGTH)
            throw new ServletException("Book name length overflow");
        if (bookDescription.length() > Limit.BOOK_DESCRIPTION_LENGTH)
            throw new ServletException("Book description length overflow");
        if (bookTags.length() > Limit.BOOK_TAGS_LENGTH)
            throw new ServletException("Book tags length overflow");

        String bookDialogEditBookId = request.getParameter("bookDialogEditBookId");
        if (bookDialogEditBookId == null)
            throw new ServletException("no bookDialogEditBookId");

        Book book;
        /*
         * New book.
         */
        if (bookDialogEditBookId.equals("")) {
            try {
                for (Book b: Book.load(user)) {
                    if (b.name.equals(bookName)) {
                        return msgcat.getString("DIALOG_CREATE_BOOK_ERROR_DUPLICATE_BOOK");
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("DB error");
            }


            book = new Book();

            book.user        = user;
            book.access      = "A";
            book.name        = bookName;
            book.description = bookDescription;
            book.icon        = ""; /* TODO */
            book.tags        = Tags.compileTags(bookTags);

            try {
                book.storeNew();
            } catch (SQLException e) {
                MyNotes.log(Level.SEVERE, "DB error, failed to store new book, " + e);
                throw new ServletException("DB error");
            } 
        } else {
            /*
             * Edit book data.
             */

            Long bookId;
            try {
                bookId = new Long(bookDialogEditBookId);
            } catch (NumberFormatException e) {
                throw new ServletException("Failed to get book ID");
            }

            try {
                book = Book.loadForUser(user, bookId);
            } catch (Exception e) {
                MyNotes.log(Level.SEVERE, "Failed to load book, id \"" + bookId + ", " + e);
                throw new ServletException("Failed to load book");
            }

            try {
                for (Book otherBook: Book.load(user)) {
                    if (book.id != otherBook.id && otherBook.name.equals(bookName)) {
                        return msgcat.getString("DIALOG_CREATE_BOOK_ERROR_DUPLICATE_BOOK");
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("DB error");
            }

            if (!book.access.equals("A"))
                throw new ServletException("No access to edit this book");

            book.name        = bookName;
            book.description = bookDescription;
            book.icon        = ""; /* TODO */
            book.tags        = Tags.compileTags(bookTags);

            try {
                book.store();
            } catch (SQLException e) {
                MyNotes.log(Level.SEVERE, "DB error, failed to store book, " + e);
                throw new ServletException("DB error");
            } 
        }

        String iconPath = null;
        try {
            String fileName  = "icon" + book.id + ".png";
            String userPath  = "user" + user.id;

            File localDir = new File(MyNotes.getInstance().bookIconPath, userPath);
            MyNotes.makeDir(localDir);
            File localFile = new File(localDir, fileName); 

            if (uploadImage(iconFilePart, localFile,
                            Limit.BOOK_ICON_WIDTH, Limit.BOOK_ICON_HEIGHT, UPLOAD_IMAGE_SCALETO))
                iconPath = "extimg/book/" + userPath + "/" + fileName;
        } catch (Exception e) {
            MyNotes.log(Level.SEVERE, "Failed to upload book icon, " + e);
            throw new ServletException("Failed to upload book icon");
        }

        if (iconPath != null) {
            book.icon = iconPath;
            try {
                book.storeIcon();
            } catch (SQLException e) {
                MyNotes.log(Level.SEVERE, "DB error, failed to store book icon, " + e);
                throw new ServletException("DB error");
            }
        }

        /* TODO remove old icon from disk if present */
        return null;
    }

    /*
     *
     */
    private String deleteBook(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());
        HttpSession session = request.getSession();

        String userName = (String)session.getAttribute("user");
        if (userName == null)
            throw new ServletException("No user in session");

        String bookIdString = request.getParameter("bookid");
        if (bookIdString == null)
            throw new ServletException("No book ID");

        Long bookId;
        try {
            bookId = new Long(bookIdString);
        } catch (NumberFormatException e) {
            throw new ServletException("Failed to convert book ID, " + e);
        }

        User user;
        try {
            user = User.loadByName(userName);
        } catch (SQLException e) {
            throw new ServletException("DB error");
        } catch (NoSuchUserException e) {
            MyNotes.log(LOGLEVEL_ERROR, "Failed to find user from session attribute, \"" + userName + "\"");
            throw new ServletException("No user");
        }

        Book book;
        try {
            book = Book.loadForUser(user, bookId);
        } catch (Exception e) {
            MyNotes.log(Level.SEVERE, "Failed to load book for user \"" + user.name + "\", bookId " + bookId);
            throw new ServletException("No user");
        }

        if (!book.access.equals("A")) {
            MyNotes.log(Level.SEVERE, "Attempt to delete book without author privilegies, user \"" + user.name + "\", bookId " + bookId);
            throw new ServletException("No access");
        }

        MyNotes.log(LOGLEVEL_INFO,
                "Delete book, id " + bookId +
                " (\"" + book.name + "\")" +
                ", by user \"" + user.name + "\"");
        try {
            Book.delete(bookId);
        } catch (SQLException e) {
            throw new ServletException("DB error," + e);
        }

        return null;
    }
    /*
     *
     */
    private String makeNote(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());
        HttpSession session = request.getSession();

        User user = null;
        try {
            user = Home.getUserFromSession(request.getSession());
        } catch (Exception e) {
            throw new ServletException("Failed to get user from session");
        }
        if (user == null)
            throw new ServletException("No user");

        String message = request.getParameter("message");
        if (message == null)
            throw new ServletException("No message");
        if (message.length() > Limit.NOTE_MESSAGE_LENGTH)
            throw new ServletException("Message length overflow");

        String tags = request.getParameter("tags");
        if (tags == null)
            throw new ServletException("No tags");
        if (tags.length() > Limit.NOTE_TAGS_LENGTH)
            throw new ServletException("Tags length overflow"); 

        String bookIdString = request.getParameter("bookid");
        Long bookId;
        if (bookIdString == null)
            throw new ServletException("No bookid");
        try {
            bookId = new Long(bookIdString);
        } catch (NumberFormatException e) {
            throw new ServletException("Invalid bookid");
        }

        Part imageFilePart = request.getPart("image");
        if (imageFilePart == null)
            throw new ServletException("No note image");

        Book book;
        try {
            book = Book.loadForUser(user, bookId);
        } catch (Exception e) {
            throw new ServletException("Failed to load book for user \"" + user.name + "\", bookId " + bookId);
        }

        String editNoteIdString = request.getParameter("noteDialogEditNoteId");
        if (editNoteIdString == null)
            throw new ServletException("no noteDialogEditNoteId");

        /*
         * New note.
         */
        Note note;
        if (editNoteIdString.equals("")) {
            if (!book.access.equals("A") && !book.access.equals("W"))
                throw new ServletException("No access to write note, user \"" + user.name + "\", bookId " + bookId);

    //        Note note = new Note(bookId, user);

            /* TODO message processing */
            /* TODO tags processing */

            note = new Note();
            note.bookId         = bookId;
            note.authorId       = user.id;
            note.lastEditUserId = user.id;
            note.creationTime   = new Date(System.currentTimeMillis());
            note.editTime       = new Date(System.currentTimeMillis());
            note.tags           = Tags.compileTags(tags);
            note.icon           = ""; /* TODO */
            note.color          = ""; /* TODO */
            note.message        = message;

            try {
                note.storeNew();
            } catch (SQLException e) {
                MyNotes.log(LOGLEVEL_ERROR, "Failed to store message to book, bookId " + bookId + ", " + e);
                throw new ServletException("Failed to store message to book, bookId " + bookId);
            }
        } else {
            /*
             * Edit note data.
             */

            Long noteId;
            try {
                noteId = new Long(editNoteIdString);
            } catch (NumberFormatException e) {
                throw new ServletException("Failed to get note ID");
            }

            try {
                note = Note.loadById(noteId);
            } catch (Exception e) {
                MyNotes.log(Level.SEVERE, "Failed to load note, id \"" + noteId + ", " + e);
                throw new ServletException("Failed to load note");
            }

            if (!(book.access.equals("A") || note.authorId == user.id))
                throw new ServletException("No access to edit this note");

            note.lastEditUserId = user.id;
            note.editTime       = new Date(System.currentTimeMillis());
            note.tags           = Tags.compileTags(tags);
            note.icon           = ""; /* TODO */
            note.color          = ""; /* TODO */
            note.message        = message;

            try {
                note.store();
            } catch (SQLException e) {
                throw new ServletException("Failed to store note, " + e);
            }
        }

        String imagePath = null;
        try {
            String userPath  = "user" + user.id;
            String fileName  = "image" + note.id + ".png";

            File localDir = new File(MyNotes.getInstance().noteImagePath, userPath);
            MyNotes.makeDir(localDir);
            File localFile = new File(localDir, fileName); 

            if (uploadImage(imageFilePart, localFile,
                        Limit.NOTE_IMAGE_MAX_WIDTH, Limit.NOTE_IMAGE_MAX_HEIGHT, UPLOAD_IMAGE_LIMITTO))
                imagePath = "extimg/note/" + userPath + "/" + fileName;
        } catch (Exception e) {
            MyNotes.log(Level.SEVERE, "Failed to upload note image, " + e);
            throw new ServletException("Failed to upload note image");
        }

        if (imagePath != null) {
            note.image = imagePath;
            try {
                note.storeImage();
            } catch (SQLException e) {
                MyNotes.log(Level.SEVERE, "DB error, failed to store note image, " + e);
                throw new ServletException("DB error");
            }
        }

        return null;
    }
    /*
     *
     */
    private String deleteNote(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());

        User user = null;
        try {
            user = Home.getUserFromSession(request.getSession());
        } catch (Exception e) {
            throw new ServletException("Failed to get user from session");
        }
        if (user == null)
            throw new ServletException("No user");

        Long noteId = Home.getLongParameter(request, "noteid");
        if (noteId == null)
            throw new ServletException("Failed to get note ID");

        Note note;
        try {
            note = Note.loadById(noteId);
        } catch (Exception e) {
            MyNotes.log(Level.SEVERE, "Failed to load note, id \"" + noteId + ", " + e);
            throw new ServletException("Failed to load note");
        }

        Book book;
        try {
            book = Book.loadForUser(user, note.bookId);
        } catch (Exception e) {
            MyNotes.log(Level.SEVERE, "Failed to load book for user \"" + user.name + "\", bookId " + note.bookId);
            throw new ServletException("No user");
        }

        if (!(book.access.equals("A") || note.authorId != user.id)) {
            MyNotes.log(Level.SEVERE,
                    "Attempt to delete note from book without author access, user \"" + user.name + "\"" +
                    ", bookId " + note.bookId +
                    ", noteId " + noteId
                    );
            throw new ServletException("No access");
        }

        MyNotes.log(LOGLEVEL_INFO,
                "Delete note, id " + note.id +
                ", book \"" + book.name + "\"" +
                ", by user \"" + user.name + "\"");
        try {
            Note.delete(note.id);
        } catch (SQLException e) {
            throw new ServletException("DB error," + e);
        }

        return null;
    }

//    /*
//     *
//     */
//    private class UploadImageTransform {
//        public Dimension dimension;
//        public int type;
//
//        final int LIMIT   = 0;
//        final int SCALETO = 1;
//
//        public UploadImageTransform(int width, int height, int scaleType) {
//            dimension = new Dimension(width, height);
//            type = scaleType;
//        }
//    }

    /*
     *
     */
    private final int UPLOAD_IMAGE_LIMITTO = 0;
    private final int UPLOAD_IMAGE_SCALETO = 1;

    private boolean uploadImage(Part filePart, File localFile, int width, int height, int transform) throws Exception {
        if (filePart.getSize() == 0)
            return false;

        BufferedImage image = null;
        try (InputStream inputStream = filePart.getInputStream()) {
            BufferedImage bImage = ImageIO.read(inputStream);

            if (transform == UPLOAD_IMAGE_SCALETO) {
                image = toBufferedImage(bImage.getScaledInstance(width, height, Image.SCALE_DEFAULT));
            } else if (transform == UPLOAD_IMAGE_LIMITTO) {
                final int PRECISION = 1000;

                int ratio = bImage.getWidth() * PRECISION / bImage.getHeight();

                int newWidth  = bImage.getWidth();
                int newHeight = bImage.getHeight();

                if (newWidth > width) {
                    newWidth  = width;
                    newHeight = width * PRECISION / ratio;
                }
                if (newHeight > height) {
                    newHeight = height;
                    newWidth  = height * ratio / PRECISION;
                }
                if (newWidth != bImage.getWidth() || newHeight != bImage.getHeight())
                    image = toBufferedImage(bImage.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT));
            } else {
                /* untransformed, XXX is it need to throw exception? */
            }
            if (image == null)
                image = bImage;
        } catch (Exception e) {
            throw new Exception("Faild to read/convert book icon, " + e);
        }

        String localFileString = localFile.toString();
        String imageFormat = localFileString.substring(localFileString.length() - 3);

        /* XXX correct jpeg bug */
        if (imageFormat.equals("jpg")) {
            int[] pixels = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

            image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        }

        ImageIO.write(image, imageFormat, localFile);
        return true;
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    private static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }    
}


