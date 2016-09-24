/*
 *
 */
package org.mynotes.servlet;

import java.util.logging.Level;
import java.util.ArrayList;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.sql.SQLException;

import java.net.URLEncoder;

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
import org.mynotes.UserState;
import org.mynotes.Book;
import org.mynotes.BookFilter;
import org.mynotes.Tags;
import org.mynotes.exceptions.NoSuchUserException;

/**
 *
 */
public final class Library extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());
        UserState userState = Home.loadUserState(request.getSession());

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();

        PrintWriter writer;
        try {
            writer = response.getWriter();
        } catch (IOException e) {
            MyNotes.log(Level.SEVERE, "PrintWriter, " + e);
            throw new ServletException("Failed to get writer");
        }

        /* Attribute was set by Home */
        User user = (User)request.getAttribute("user");
        if (user == null)
        {
            writer.println("<span class=\"error\">No user</span>");
            return;
        }

        BookFilter bookFilter = new BookFilter();
        synchronized (userState) {
            if (!userState.bookTags.isEmpty())
                bookFilter.tagFilter = new Tags(userState.bookTags);
        }

        ArrayList<Book> books = null;
        try {
            books = Book.load(user, bookFilter);
        } catch (SQLException e) {
            MyNotes.log(Level.SEVERE, "Failed to load books for userId " + user.id + ", " + e);
            writer.println("<span class=\"error\">DB error</span>");
            return;
        }

        if (books == null || books.size() == 0) {
            StringBuilder sb = new StringBuilder();

            sb.append("<span>");
            sb.append(msgcat.getString("LIBRARY_NO_BOOKS"));
            sb.append("</span>");
            writer.println(sb.toString());
        } else {
            /*
             * Tag filter block.
             */
            {
                boolean isEmpty;
                synchronized (userState) {
                    isEmpty = userState.bookTags.isEmpty();
                }

                if (!isEmpty) {
                    writer.println("<div class=\"tag_filter\">");
                    writer.print("<div class=\"note_control\">");
                    Home.putImageButton(writer, "pager_button_block_item",
                            "img/buttons/delete.png", "delete", msgcat.getString("PAGER_CLEAR_TAGS"), "bookTagControl.exec('clear')");
                    writer.println("</div>");

                    ArrayList<String> tags;
                    synchronized (userState) {
                        tags = new ArrayList<>(userState.bookTags.get());
                    }
                    for (String tag : tags) {
                        writer.println("<span class=\"note_tag_remove_link\"" +
                                " onclick=\"bookTagControl.exec('remove', '" + tagConvert(tag.substring(1)) + "')\"" +
                                ">" + tag + "</span>");
                    }
                    writer.println("</div>");
                }
            }

            writer.println("<div class=\"table\">");
            for (Book book: books) {
                writer.println("<div class=\"library_book_entry\">");
                writer.print("    <div class=\"library_book_thumb\">");

                String thumb;
                if (book.icon == null || book.icon.equals(""))
                    thumb = "img/book_thumb.png";
                else
                    thumb = book.icon;

                writer.print("<a href=\"home?nav=books&bookid=" + book.id + "\"><img class=\"library_book_border\" alt=\"" + book.name + "\" title=\"" + book.name + "\" src=\"" + thumb + "\"></a>");
                writer.println("    </div>");
                writer.println("    <div class=\"library_book_info\">");
                writer.println("        <div class=\"library_book_info_table\">");
                writer.println("            <div class=\"library_book_info_row\">");
                writer.println("                <div class=\"library_book_info_cell library_book_info_caption\">" + msgcat.getString("LIBRARY_NAME") + "</div>");
                writer.println("                <div class=\"library_book_info_cell\"><a href=\"home?nav=books&bookid=" + book.id + "\">" + book.name + "</a></div>");
                writer.println("            </div>");
                writer.println("            <div class=\"library_book_info_row\">");
                writer.println("                <div class=\"library_book_info_cell library_book_info_caption\">" + msgcat.getString("LIBRARY_AUTHOR") + "</div>");
                {
                    String authorString = "<span class=\"error\">DB error</span>";
                    try {
                        User author = book.getAuthor();
                        authorString = author.name;
                    } catch (Exception e) {
                        MyNotes.log(Level.SEVERE, "Failed to get author, " + e);
                    }
                    writer.println("                <div class=\"library_book_info_cell\">" + authorString + "</div>");
                }
                writer.println("            </div>");
                writer.println("            <div class=\"library_book_info_row\">");
                writer.println("                <div class=\"library_book_info_cell library_book_info_caption\">" + msgcat.getString("LIBRARY_DESCRIPTION") + "</div>");
                writer.println("                <div class=\"library_book_info_cell\">" + book.description + "</div>");
                writer.println("            </div>");

                writer.println("            <div class=\"library_book_info_row\">");
                writer.println("                <div class=\"library_book_info_cell library_book_info_caption\">" + msgcat.getString("LIBRARY_ACCESS") + "</div>");
                switch (book.access) {
                    case "A":
                        writer.println("                <div class=\"library_book_info_cell\">" + msgcat.getString("LIBRARY_ACCESS_A") + "</div>");
                        break;
                    case "R":
                        writer.println("                <div class=\"library_book_info_cell\">" + msgcat.getString("LIBRARY_ACCESS_R") + "</div>");
                        break;
                    case "W":
                        writer.println("                <div class=\"library_book_info_cell\">" + msgcat.getString("LIBRARY_ACCESS_W") + "</div>");
                        break;
                    default:
                        writer.println("                <div class=\"library_book_info_cell\">" + "-" + "</div>");
                        break;
                }
                writer.println("            </div>");
                writer.println("            <div class=\"library_book_info_row\">");
                writer.println("                <div class=\"library_book_info_cell library_book_info_caption\">" + msgcat.getString("LIBRARY_TAGS") + "</div>");
//                writer.println("                <div class=\"library_book_info_cell\">" + book.tags + "</div>");
                writer.println("                <div class=\"library_book_info_cell\">" + compileTags(book) + "</div>");
                writer.println("            </div>");
                writer.println("        </div>");
                writer.println("    </div>");

                writer.println("    <div class=\"library_button_block\">");
                if (book.access.equals("A")) {
                    Home.putImageButton(writer, "library_button_block_item",
                            "img/buttons/delete.png", "delbook", msgcat.getString("LIBRARY_DELETE_BOOK"), "click_button_delete_book(" + book.id + ")");
                    Home.putImageButton(writer, "library_button_block_item",
                            "img/buttons/edit.png", "editbook", msgcat.getString("LIBRARY_EDIT_BOOK"), "click_button_edit_book(" + book.id + ")");
                }
                writer.println("    </div>");

                writer.println("</div>");
            }
            writer.println("</div>");
        }

//        for (int i = 0; i < 100; i++)
//            writer.println("<p>Test" + i + "</p>");

        /* NOTE XXX don't close output stream */
    }
    /*
     *
     */
    private String compileTags(Book book) {
//        StringBuilder output = new StringBuilder("<div class=\"note_tags\">");
        StringBuilder output = new StringBuilder();

        for (String tag: book.tags.get()) {
            if (tag.length() > 1) {
                /* XXX is there need to HTMLEscape tags here? */

                String tagParam = tagConvert(tag.substring(1));

                output.append("<span class=\"note_tag_link\"" +
                        " onclick=\"bookTagControl.exec('add', '" + tagParam + "')\"" +
                        ">" + tag + "</span> " /* NOTE space after span has meaning (for proper word break) */ );

//                output.append(tag + " ");
//                output.append("<span class=\"note_tag_link\"" +
//                        " onclick=\"noteManager.addRemoveTag('add', '" + tagParam + "')\"" +
//                        ">" + tag + "</span> " /* NOTE space after span has meaning (for proper word break) */ );
            }
        }

//        output.append("</div>");
        return output.toString();
    }
    /*
     *
     */
    private String tagConvert(String tag) {
        try {
            tag = URLEncoder.encode(tag, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            MyNotes.log(Level.SEVERE, "Failed to convert tag, unsupported encoding " + e);
        }

        return tag;
    }

}

//            request.getRequestDispatcher("WEB-INF/login.html").include(request, response);

