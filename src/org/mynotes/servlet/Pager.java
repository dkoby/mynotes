/*
 *
 */
package org.mynotes.servlet;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.text.SimpleDateFormat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.sql.SQLException;

import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mynotes.MyNotes;
import org.mynotes.MyUtil;
import org.mynotes.UserState;
import org.mynotes.User;
import org.mynotes.Book;
import org.mynotes.Note;
import org.mynotes.Tags;
import org.mynotes.NoteFilter;
import org.mynotes.Limit;

/**
 *
 */
public final class Pager extends HttpServlet {
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

        User user = null;
        try {
            user = Home.getUserFromSession(session);
        } catch (Exception e) {
            throw new ServletException("Failed to get user from session");
        }
        if (user == null) {
            throw new ServletException("No user");
        }


        String bookIdString = request.getParameter("bookid");
        Long bookId = null;
        try {
            bookId = new Long(bookIdString);
        } catch (NumberFormatException e) {
            writer.println("<span class=\"error\">Invalid book id</span>");
            return;
        }

        /*
         * Get page number.
         */
        Long pageNum = Home.getLongParameter(request, "pagenum");
//        String prevPage = request.getParameter("prevpage");
//        String nextPage = request.getParameter("nextpage");
//        if (prevPage != null && pageNum != 0 && pageNum > 0)
//            pageNum--;
//        else if (nextPage != null && pageNum != 0)
//            pageNum++;

        Book book;
        try {
            book = Book.loadForUser(user, bookId);
        } catch (Exception e) {
            writer.println("<span class=\"error\">DB error/no access</span>");
            MyNotes.log(Level.SEVERE, "Failed to load book for user \"" + user.name + "\", bookId " + bookId + ", " + e);
            return;
        }

        /*
         * Set title.
         */
        writer.println("<script type=\"text/javascript\">");
        {
            StringBuilder sb = new StringBuilder();
            sb.append(msgcat.getString("TITLE_BOOK"));
            sb.append(": " + book.name);
            writer.println("document.title=\"" + sb + "\"");
        }
        writer.println("</script>");

        writer.println("<div class=\"notes_book_caption\">" + book.name + "</div>");

        boolean isEmpty;
        synchronized (userState) {
            isEmpty = userState.noteTags.isEmpty();
        }

        if (!isEmpty) {
            writer.println("<div class=\"tag_filter\">");
            writer.print("<div class=\"note_control\">");
            Home.putImageButton(writer, "pager_button_block_item",
                    "img/buttons/delete.png", "delete", msgcat.getString("PAGER_CLEAR_TAGS"), "noteTagControl.exec('clear')");
            writer.println("</div>");

            ArrayList<String> tags;
            synchronized (userState) {
                tags = new ArrayList<>(userState.noteTags.get());
            }
            for (String tag : tags) {
                writer.println("<span class=\"note_tag_remove_link\"" +
                        " onclick=\"noteTagControl.exec('remove', '" + tagConvert(tag.substring(1)) + "')\"" +
                        ">" + tag + "</span>");
            }
            writer.println("</div>");
        }

        NoteFilter noteFilter = new NoteFilter(pageNum, Limit.NOTES_PER_PAGE);
        synchronized (userState) {
            if (!userState.noteTags.isEmpty())
                noteFilter.tagFilter = new Tags(userState.noteTags);
        }

        ArrayList<Note> notes;
        try {
           notes = Note.loadForBook(book, noteFilter);
        } catch (SQLException e) {
            MyNotes.log(Level.SEVERE, "Failed to load notes, bookId " + bookId + ", " + e);
            writer.println("<span class=\"error\">DB error</span>");
            return;
        }


        if (notes.size() < 1) {
            writer.println("<span>" + msgcat.getString("PAGER_NO_MESSAGES_IN_THIS_BOOK") + "</span>");
            return;
        }

        for (Note note: notes) {
            writer.println("<div class=\"note\" >");

            writer.println("<div class=\"note_control\">");
            /* TODO check access */
            Home.putImageButton(writer, "pager_button_block_item",
                    "img/buttons/edit.png", "edit", msgcat.getString("PAGER_EDIT_NOTE"), "click_button_edit_note(" + note.id + ")");
            Home.putImageButton(writer, "pager_button_block_item",
                    "img/buttons/delete.png", "delete", msgcat.getString("PAGER_DELETE_NOTE"), "click_button_delete_note(" + note.id + ", " + note.bookId + ")");
            writer.println("</div>");

            String image;
            if (note.image != null && !note.image.equals("")) {
                writer.print("<div class=\"note_image_block\"><img class=\"note_image\" alt=\"Note image\" src=\"" + note.image + "\"></div>");
            }

            writer.print(compileMessage(note, msgcat));
            writer.print(compileTags(note));

            writer.println("<div class=\"note_info\">");
            {
                writer.println("    <div>");
                writer.print("<div class=\"note_info_caption\">" + msgcat.getString("PAGER_AUTHOR") + "</div>");
                writer.print("<div>" + user.name + "</div>");
                writer.println("    </div>");
            }
            {
                writer.println("    <div>");
                writer.print("<div class=\"note_info_caption\">" + msgcat.getString("PAGER_CREATION_TIME") + "</div>");
                writer.print("<div>" + new SimpleDateFormat("HH:mm:ss, dd LLL yyyy", request.getLocale()).format(note.creationTime) + "</div>");
                writer.println("    </div>");
            }
            if (!note.editTime.equals(note.creationTime))
            {
                writer.println("    <div>");
                writer.print("<div class=\"note_info_caption\">" + msgcat.getString("PAGER_EDIT_TIME") + "</div>");
                writer.print("<div>" + new SimpleDateFormat("HH:mm:ss, dd LLL yyyy", request.getLocale()).format(note.editTime) + "</div>");
                writer.println("    </div>");
            }
            if (note.lastEditUserId != note.authorId)
            {
                User editUser;

                try {
                    editUser = User.loadById(note.lastEditUserId);
                } catch (Exception e) {
                    MyNotes.log(Level.SEVERE, "Failed to user, id " + note.lastEditUserId + ", " + e);
                    writer.println("<span class=\"error\">DB error</span>");
                    return;
                }

                writer.println("    <div>");
                writer.print("<div class=\"note_info_caption\">" + msgcat.getString("PAGER_EDIT_AUTHOR") + "</div>");
                writer.print("<div>" + editUser.name + "</div>");
                writer.println("    </div>");
            }
            writer.println("</div>");

            writer.println("</div>");
        }

        if (notes.size() >= 1) {
            long currentPage = noteFilter.pageInfo.page;
            long pageCount   = noteFilter.pageInfo.pages;
            long toPage;

            writer.print("<div class=\"note_page_control_block\">");

            String href = "home?nav=books&bookid=" + book.id + "&pagenum=";

            final String EMPTY_BUTTON = "img/buttons/empty18.png";
            /* to first page */
            if (true) {
//            if (pageCount > 1 && currentPage != 0) {
                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/pagelimit.png", "<", msgcat.getString("PAGER_FIRST_PAGE"),
                        "follow_link('" + href + 0 + "')");
            } else {
                Home.putImageButton(writer, "pager_button_block_item note_page_control", EMPTY_BUTTON, null, null, null);
            }
            /* -100 pages */
            {
                if (currentPage > 100)
                    toPage = currentPage - 100;
                else
                    toPage = 0;

                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/prev100.png", "<<<", msgcat.getString("PAGER_PREV100PAGE"),
                        "follow_link('" + href + toPage + "')");
            }
            /* -10 pages */
            {
                if (currentPage > 10)
                    toPage = currentPage - 10;
                else
                    toPage = 0;

                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/prev10.png", "<<", msgcat.getString("PAGER_PREV10PAGE"),
                        "follow_link('" + href + toPage + "')");
            }
            /* -1 page */
            {
                if (currentPage > 1)
                    toPage = currentPage - 1;
                else
                    toPage = 0;

                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/prev.png", "<", msgcat.getString("PAGER_PREVPAGE"),
                        "follow_link('" + href + toPage + "')");
            }
            /* + 1 page */
            {
                if (currentPage < (pageCount - 1))
                    toPage = currentPage + 1;
                else
                    toPage = pageCount - 1;

                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/next.png", ">", msgcat.getString("PAGER_NEXTPAGE"),
                        "follow_link('" + href + toPage + "')");
            }
            /* + 10 page */
            {
                if ((currentPage + 10) < (pageCount - 1))
                    toPage = currentPage + 10;
                else
                    toPage = pageCount - 1;

                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/next10.png", ">>", msgcat.getString("PAGER_NEXT10PAGE"),
                        "follow_link('" + href + toPage + "')");
            }
            /* + 100 page */
            {
                if ((currentPage + 100) < (pageCount - 1))
                    toPage = currentPage + 100;
                else
                    toPage = pageCount - 1;

                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/next100.png", ">>>", msgcat.getString("PAGER_NEXT100PAGE"),
                        "follow_link('" + href + toPage + "')");
            }
            /* to last page */
            if (true) {
//            if (pageCount > 1 && currentPage != (pageCount - 1)) {
                Home.putImageButton(writer, "pager_button_block_item note_page_control",
                        "img/buttons/pagelimit.png", "<", msgcat.getString("PAGER_LAST_PAGE"),
                        "follow_link('" + href + (pageCount - 1) + "')");
            } else {
                Home.putImageButton(writer, "pager_button_block_item note_page_control", EMPTY_BUTTON, null, null, null);
            }

            writer.print("&nbsp;<input");
            writer.print(" class=\"note_page_control\"");
            writer.print(" id=\"pageInput\"");
//            writer.print(" form=\"makebook\"");
            writer.print(" type=\"text\"");
            writer.print(" name=\"pageinput\"");
            writer.print(" autocomplete=\"off\"");
            writer.print(" size=\"" + 4 + "\"");
            writer.print(" minlength=\"1\"");
            writer.print(" maxlength=\"10\"");
            writer.print(">");

            writer.print("<span");
            writer.print(" class=\"note_page_input_button button_dark\"");
            writer.print(" id=\"pageInputButton\"");
            writer.print(" onclick=\"noteManager.goToPage();\"");
            writer.print(">OK</span>");

            writer.print("</div>");

            writer.print("<div class=\"note_page_control_block\">");
            /* page number */
            {
                long page  = currentPage + 1;
                long pages = pageCount;

                writer.print("<span class=\"note_page_control\">" +
                        msgcat.getString("PAGER_PAGE") + " " + page + "/" + pages);
            }
            writer.print("</div>");
        }

        writer.println("<script src=\"js/note.js\"></script>");
        /* NOTE XXX don't close output stream */
    }

    /*
     *
     */
    private String compileMessage(Note note, MyNotes.MsgCat msgcat) {
        StringBuilder output = new StringBuilder("<pre class=\"note_message\" id=\"note" + note.id + "\">");

        boolean error = false;
        int codeBlock = 0;
        int lineNumber = 0;
        for (String line: note.message.split("\n")) {
            lineNumber++;

            Matcher matcher;
            line = MyUtil.HTMLEscape(line);

            /* XXX is infinite loop possible? */
            while (true) {
                /*
                 * [url="http://..."]Link[/url]
                 */
                matcher = Pattern.compile("(.*?)\\[url=&quot;(.*)&quot;\\](.*?)\\[/url\\](.*)").matcher(line);
                if (matcher.find()) {
                    StringBuilder subst = new StringBuilder();
                    /* XXX TODO check all matches indexes */

                    String before  = matcher.group(1);
                    String url     = matcher.group(2);
                    String urlText = matcher.group(3);
                    String after   = matcher.group(4);

                    subst.append(before);
                    subst.append("<a class=\"note_link\" href=\"" + url + "\">" + urlText + "</a>");
                    subst.append(after);

                    line = subst.toString();
                    continue;
                }
                /*
                 * Line through text.
                 * [lt]Text[/lt]
                 */
                matcher = Pattern.compile("(.*?)\\[lt\\](.*?)\\[/lt\\](.*)").matcher(line);
                if (matcher.find()) {
                    StringBuilder subst = new StringBuilder();
                    /* XXX TODO check all matches indexes */

                    String before = matcher.group(1);
                    String text   = matcher.group(2);
                    String after  = matcher.group(3);

                    subst.append(before);
                    subst.append("<span class=\"compiller_line_through\">" + text + "</span>");
                    subst.append(after);

                    line = subst.toString();
                    continue;
                }
                /*
                 * Underline text.
                 * [ul]Text[/ul]
                 */
                matcher = Pattern.compile("(.*?)\\[ul\\](.*?)\\[/ul\\](.*)").matcher(line);
                if (matcher.find()) {
                    StringBuilder subst = new StringBuilder();
                    /* XXX TODO check all matches indexes */

                    String before = matcher.group(1);
                    String text   = matcher.group(2);
                    String after  = matcher.group(3);

                    subst.append(before);
                    subst.append("<span class=\"compiller_underline\">" + text + "</span>");
                    subst.append(after);

                    line = subst.toString();
                    continue;
                }

                /*
                 * Bold text.
                 * [b]Text[/b]
                 */
                matcher = Pattern.compile("(.*?)\\[b\\](.*?)\\[/b\\](.*)").matcher(line);
                if (matcher.find()) {
                    StringBuilder subst = new StringBuilder();
                    /* XXX TODO check all matches indexes */

                    String before = matcher.group(1);
                    String text   = matcher.group(2);
                    String after  = matcher.group(3);

                    subst.append(before);
                    subst.append("<span class=\"compiller_bold\">" + text + "</span>");
                    subst.append(after);

                    line = subst.toString();
                    continue;
                }
                /*
                 * Code block start.
                 * [cb]
                 */
                matcher = Pattern.compile("(.*?)\\[cb\\](.*)").matcher(line);
                if (matcher.find()) {
                    StringBuilder subst = new StringBuilder();
                    /* XXX TODO check all matches indexes */

                    String before = matcher.group(1);
                    String after  = matcher.group(2);

                    subst.append(before);
                    subst.append("<span class=\"compiller_code_block\">");
                    subst.append(after);

                    line = subst.toString();

                    codeBlock++;
                    continue;
                }
                /*
                 * Code block end.
                 * [/cb]
                 */
                matcher = Pattern.compile("(.*?)\\[/cb\\](.*)").matcher(line);
                if (matcher.find()) {
                    StringBuilder subst = new StringBuilder();
                    /* XXX TODO check all matches indexes */

                    if (codeBlock > 0) {
                        String before = matcher.group(1);
                        String after  = matcher.group(2);

                        subst.append(before);
                        subst.append("</span>");
                        subst.append(after);

                        line = subst.toString();

                        codeBlock--;
                    } else {
                        error = true;
                        break;
                    }
                    continue;
                }
                /*
                 * Unicode character
                 * [uchar="XXXX"]
                 */
                matcher = Pattern.compile("(.*?)\\[uchar=&quot;([0-9a-zA-Z]+)&quot;\\](.*)").matcher(line);
                if (matcher.find()) {
                    StringBuilder subst = new StringBuilder();
                    /* XXX TODO check all matches indexes */

                    String before = matcher.group(1);
                    String num    = matcher.group(2);
                    String after  = matcher.group(3);

                    subst.append(before);
                    subst.append("<span>&#x" + num + ";</span>");
                    subst.append(after);

                    line = subst.toString();
                    continue;
                }
                break;
            }

            output.append(line + "\n");
        }

        if (codeBlock < 0 || codeBlock > 0) {
            output = new StringBuilder("<span class=\"error\">" + 
                    msgcat.getString("ERROR") + ", [cb]" +
                    "</span>");

        }

        if (!error) {
            output.append("</pre>");
        } else {
            output = new StringBuilder("<span class=\"error\">" + 
                    msgcat.getString("ERROR") + ", " +
                    msgcat.getString("PAGER_COMPILLER_CHECK_SYNTAX_AT_LINE") + " " + lineNumber +
                    "</span>");
        }
        return output.toString();
    }
    /*
     *
     */
    private String compileTags(Note note) {
        StringBuilder output = new StringBuilder("<div class=\"note_tags\">");

        for (String tag: note.tags.get()) {
            if (tag.length() > 1) {
                /* XXX is there need to HTMLEscape tags here? */

                String tagParam = tagConvert(tag.substring(1));

//                output.append(tag + " ");
                output.append("<span class=\"note_tag_link\"" +
                        " onclick=\"noteTagControl.exec('add', '" + tagParam + "')\"" +
                        ">" + tag + "</span> " /* NOTE space after span has meaning (for proper word break) */ );
            }
        }

        output.append("</div>");
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

