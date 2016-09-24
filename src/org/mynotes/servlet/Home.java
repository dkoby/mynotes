/*
 *
 */
package org.mynotes.servlet;

import java.util.logging.Level;

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

import org.mynotes.MyNotes;
import org.mynotes.UserState;
import org.mynotes.User;
import org.mynotes.Limit;
import org.mynotes.exceptions.NoSuchUserException;

/**
 *
 */
public final class Home extends HttpServlet {
    public static final String BUTTON_IMAGE_CREATE_BOOK    = "img/buttons/newbook.png";
    public static final String BUTTON_IMAGE_CREATE_MESSAGE = "img/buttons/newmessage.png";
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());

        HttpSession session = request.getSession();

        User user = null;
        try {
            user = getUserFromSession(session);
        } catch (Exception e) {
            throw new ServletException("Failed to get user from session");
        }
        if (user == null)
        {
            response.sendRedirect("login");
            return;
        }

        /* NOTE pass User instance to descendant pages */
        request.setAttribute("user", user);

        /* Set default navigation to "books" */
        String nav = request.getParameter("nav");
        String bookIdString = request.getParameter("bookid");
        if (nav == null)
            nav = "books";

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<title>");
            {
                StringBuilder sb = new StringBuilder();
                sb.append(msgcat.getString("APPLICATION_NAME"));
                if (nav.equals("books")) {
                    sb.append(", ");
                    sb.append(msgcat.getString("TITLE_BOOKS"));
                }
                writer.println(sb.toString());
            }
            writer.println("</title>");
            writer.println("<link rel=\"stylesheet\" href=\"css/home.css\">");
            writer.println("<script src=\"js/generic.js\"></script>");
            writer.println("</head>");

            writer.println("<body>");

            writer.println("<div class=\"home_control with_border\">");
            writer.println("    <span class=\"user_head\">");
//            writer.print("&#x270E" + user.getName() + " ");
            writer.print("<strong>" + user.name + "</strong></span>");
            writer.print("<a href=\"login?logout\">" + msgcat.getString("LOGOUT") + "</a>");

            writer.println("    <div class=\"menu\">");
            {
                class MenuItem {
                    String name;
                    String href;
                    boolean selected;
                    MenuItem(String name, String href, boolean selected) {
                        this.name = name;
                        this.href = href;
                        this.selected = selected;
                    }
                }
                MenuItem[] menu = new MenuItem[] {
                    new MenuItem(msgcat.getString("MENU_ITEM_BOOKS")      , "home?nav=books",  nav.equals("books")  ? true : false),
//                    new MenuItem(msgcat.getString("MENU_ITEM_PREFERENCES"), "home?nav=pref",   nav.equals("pref")   ? true : false),
//                    new MenuItem(msgcat.getString("MENU_ITEM_SEARCH")     , "home?nav=search", nav.equals("search") ? true : false),
                };
                String tab = "        ";
                for (MenuItem menuItem : menu) {
                    StringBuilder sb = new StringBuilder(tab);
                    sb.append("<a class=\"menu_item\"");
                    sb.append(" href=\"" + menuItem.href + "\"");
                    sb.append(">");
                    if (menuItem.selected)
                        sb.append("&#x2770<strong>");
                    sb.append(menuItem.name);
                    if (menuItem.selected)
                        sb.append("&#x2771</strong>");
                    sb.append("</a>");
                    writer.println(sb.toString());
                }
            }
            writer.println("    </div>");
            writer.println("</div>");

            writer.println("<div class=\"home_workspace with_border\">");

            /*
             * Header controls
             */
            writer.println("    <div class=\"home_workspace_header_control\">");
            switch (nav) {
                case "books":
                    if (bookIdString == null)
                        putImageButton(writer, "img_button", BUTTON_IMAGE_CREATE_BOOK, "+book", msgcat.getString("CONTROL_CREATE_NEW_BOOK"), "click_button_create_book()");
                    else
                        putImageButton(writer, "img_button", BUTTON_IMAGE_CREATE_MESSAGE, "+message", msgcat.getString("CONTROL_CREATE_NEW_NOTE"), "click_button_create_note()");
                    break;
            }
            writer.println("    </div>");

            /*
             * Main workspace
             */
            writer.println("    <div class=\"home_workspace_main\">");
            switch (nav) {
                case "books":
                    if (bookIdString == null)
                        request.getRequestDispatcher("library").include(request, response);
                    else
                        request.getRequestDispatcher("pager").include(request, response);
                    break;
            }

//            writer.println("<div class=\"symbol_icon\" title=\"" + msgcat.getString("CONTROL_TO_BOTTOM") + "\" onclick=\"window.scrollTo(0, document.body.scrollHeight)\">&#x25bf</div>");
//            writer.println("<div class=\"symbol_icon\" title=\"" + msgcat.getString("CONTROL_TO_TOP") + "\"  onclick=\"window.scrollTo(0, 0)\">&#x25b5</div>");
            writer.println("    </div>");

            writer.println("</div>"); /* home_workspace */

            /*
             * Overlays
             */
            writer.println("<div id=\"overlay\" class=\"popup overlay overlay_dark\" onclick=\"generic.overlayClose()\"></div>");
            writer.println("<div id=\"overlay_loading\" class=\"overlay_loading\"></div>");
            bookDialog(session, writer, msgcat);
            deleteBookDialog(session, writer, msgcat);
            if (bookIdString != null) {
                Long bookId = null;

                try {
                    bookId = new Long(bookIdString);
                } catch (NumberFormatException e) {
                    MyNotes.log(Level.SEVERE, "Failed to convert bookId to long");
                }
                if (bookId != null)
                    noteDialog(session, writer, msgcat, bookId);
            }
            deleteNoteDialog(session, writer, msgcat);

            writer.println("</body>");
            writer.println("<script src=\"js/home.js\"></script>");
            writer.println("</html>");
        }
    }
    /*
     *
     */
    public static void putImageButton(PrintWriter writer, String cssClass, String imgPath, String alt, String title, String action) {
        writer.print("<img class=\"" + cssClass + "\"");
        if (action != null)
            writer.print(" onclick=\"" + action + "\"");
        writer.print(" src=\"" + imgPath + "\"");
        if (alt != null)
            writer.print(" alt=\"" + alt + "\"");
        if (title != null)
            writer.print(" title=\"" + title + "\"");
        writer.print(" onmouseover=\"generic.imageOnMouseOver(this)\"");
        writer.print(" onmouseout=\"generic.imageOnMouseOut(this)\"");
        writer.print(">");
//        writer.println("");
    }
    /*
     *
     */
    private void deleteBookDialog(HttpSession session, PrintWriter writer, MyNotes.MsgCat msgcat) {
        writer.println("<div id=\"bookDeleteDialog\" class=\"popup overlay_dialog with_border\" onclick=\"stop_propagation(event)\">");

        writer.println("    <table>");

        writer.println("        <tr>");
        writer.print("              <td colspan=\"2\">");
        writer.print("<span id=\"bookDeleteDialogErrorOutput\" class=\"error\"></span>");
        writer.println("</td>");
        writer.println("        </tr>");

        writer.println("            <tr>");
        writer.print("                <td align=\"center\">");
        {
            writer.print("<button");
            writer.print(" id=\"bookDeleteDialogButton\"");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"click_button_delete_book_proceed()\"");
            writer.print(" tabindex=\"2\"");
            writer.print(">");
            writer.print(msgcat.getString("CONFIRM"));
            writer.println("</button>");
        }
        writer.println("</td>");
        writer.print("                <td align=\"center\">");
        {
            writer.print("<button");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"generic.overlayClose()\"");
            writer.print(" tabindex=\"3\"");
            writer.print(" onfocus=\"this.tabIndex=1\"");
            writer.print(" onblur=\"this.tabIndex=2\"");
            writer.print(">");
            writer.print(msgcat.getString("CANCEL"));
            writer.println("</button>");
        }
        writer.println("</td>");
        writer.println("            </tr>");

        writer.println("    </table>");
        writer.println("</div>");
    }


    /*
     *
     */
    private void bookDialog(HttpSession session, PrintWriter writer, MyNotes.MsgCat msgcat) {
        writer.println("<div id=\"bookDialog\" class=\"popup overlay_dialog with_border\" onclick=\"stop_propagation(event)\">");
        writer.print("    <form ");
        writer.print(" id=\"makebook\"");
        writer.print(" action=\"formaction\"");
        writer.print(" method=\"post\"");
        writer.print(" accept-charset=\"utf-8\"");
        writer.print(" enctype=\"multipart/form-data\"");
        writer.print("></form>");
        writer.println("");

        writer.print("<input");
        writer.print(" form=\"makebook\"");
        writer.print(" type=\"hidden\"");
        writer.print(" name=\"type\"");
        writer.print(" value=\"makebook\"");
        writer.println(">");

        writer.print("<input");
        writer.print(" form=\"makebook\"");
        writer.print(" type=\"hidden\"");
        writer.print(" id=\"bookDialogEditBookId\"");
        writer.print(" name=\"bookDialogEditBookId\"");
        writer.print(" value=\"\"");
        writer.println(">");

        writer.println("    <table>");

        final int INPUT_LENGTH_CHARS = 30;
        int tabindex = 2;

        writer.println("        <tr>");
        writer.print("              <td>" + msgcat.getString("DIALOG_CREATE_BOOK_NAME") + "</td>");
        {
            writer.print("<td>");
            writer.print("<input");
            writer.print(" id=\"bookDialogName\"");
            writer.print(" form=\"makebook\"");
            writer.print(" type=\"text\"");
            writer.print(" name=\"name\"");
            writer.print(" autocomplete=\"off\"");
            writer.print(" size=\"" + INPUT_LENGTH_CHARS + "\"");
            writer.print(" minlength=\"1\"");
            writer.print(" maxlength=\"40\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(">");
            writer.print("</td>");
            writer.println("");
        }
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.print("              <td>" + msgcat.getString("DIALOG_CREATE_BOOK_DESCRIPTION") + "</td>");
        {
            writer.print("<td>");
            writer.print("<textarea");
            writer.print(" id=\"bookDialogDescription\"");
            writer.print(" form=\"makebook\"");
            writer.print(" class=\"box\"");
            writer.print(" name=\"description\"");
            writer.print(" cols=\"" + INPUT_LENGTH_CHARS + "\"");
            writer.print(" rows=\"5\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(" maxlength=\"512\"");
            writer.print("></textarea>");
            writer.print("</td>");
            writer.println("");
        }
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.print("              <td>" + msgcat.getString("DIALOG_CREATE_BOOK_TAGS") + "</td>");
        {
            writer.print("<td>");
            writer.print("<input");
            writer.print(" id=\"bookDialogTags\"");
            writer.print(" form=\"makebook\"");
            writer.print(" type=\"text\"");
            writer.print(" name=\"tags\"");
            writer.print(" autocomplete=\"off\"");
            writer.print(" size=\"" + INPUT_LENGTH_CHARS + "\"");
            writer.print(" maxlength=\"\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(">");
            writer.print("</td>");
            writer.println("");
        }
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.print("              <td>" + msgcat.getString("DIALOG_CREATE_BOOK_ICON") + "</td>");
        {
            writer.print("<td>");
            writer.print("<input");
            writer.print(" id=\"bookDialogIcon\"");
            writer.print(" form=\"makebook\"");
            writer.print(" type=\"file\"");
            writer.print(" name=\"icon\"");
            writer.print(" accept=\".jpg,.png\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(">");
            writer.print("</td>");
            writer.println("");
        }
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.print("              <td colspan=\"2\">");
        writer.print("<span id=\"bookDialogErrorOutput\" class=\"error\"></span>");
        writer.println("</td>");
        writer.println("        </tr>");

        writer.println("            <tr>");
        writer.println("            <td>");
        writer.println("            </td>");
        writer.print("                <td align=\"center\">");
        {
            writer.print("<button");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"click_button_create_book_proceed()\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(">");
            writer.print(msgcat.getString("CONFIRM"));
            writer.println("</button>");
        }
        {
            writer.print("<button");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"generic.overlayClose()\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(" onfocus=\"this.tabIndex=1\"");
            writer.print(" onblur=\"this.tabIndex=" + (tabindex - 2) + "\"");
            writer.print(">");
            writer.print(msgcat.getString("CANCEL"));
            writer.println("</button>");
        }

        writer.println("</td>");
        writer.println("            </tr>");


//<textarea name="textarea" style="width:250px;height:150px;"></textarea>        

        writer.println("    </table>");
        writer.println("</div>");
    }
    /*
     *
     */
    private void noteDialog(HttpSession session, PrintWriter writer, MyNotes.MsgCat msgcat, Long bookId) {
        writer.println("<div id=\"noteDialog\" class=\"popup overlay_dialog with_border\" onclick=\"stop_propagation(event)\">");
        writer.print("    <form ");
        writer.print(" id=\"makenote\"");
        writer.print(" action=\"formaction\"");
        writer.print(" method=\"post\"");
        writer.print(" accept-charset=\"utf-8\"");
        writer.print("></form>");
        writer.println("");

        writer.print("<input");
        writer.print(" form=\"makenote\"");
        writer.print(" type=\"hidden\"");
        writer.print(" name=\"type\"");
        writer.print(" value=\"makenote\"");
        writer.println(">");

        writer.print("<input");
        writer.print(" form=\"makenote\"");
        writer.print(" type=\"hidden\"");
        writer.print(" name=\"bookid\"");
        writer.print(" value=\"" + bookId + "\"");
        writer.println(">");

        writer.print("<input");
        writer.print(" form=\"makenote\"");
        writer.print(" type=\"hidden\"");
        writer.print(" id=\"noteDialogEditNoteId\"");
        writer.print(" name=\"noteDialogEditNoteId\"");
        writer.print(" value=\"\"");
        writer.println(">");

        writer.println("    <table>");

        final int INPUT_LENGTH_CHARS = 80;

        int tabindex = 2;
        writer.println("        <tr>");
        writer.print("              <td class=\"overlay_item_name\">" + msgcat.getString("DIALOG_CREATE_NOTE_MESSAGE") + "</td>");
        {
            writer.print("<td>");
            writer.print("<textarea");
            writer.print(" id=\"noteDialogMessage\"");
            writer.print(" class=\"box\"");
            writer.print(" form=\"makenote\"");
            writer.print(" name=\"message\"");
            writer.print(" cols=\"" + INPUT_LENGTH_CHARS + "\"");
            writer.print(" rows=\"25\"");
            writer.print(" spellcheck=\"false\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(" maxlength=\"" + Limit.NOTE_MESSAGE_LENGTH + "\"");
            writer.print("></textarea>");
        }
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.print("              <td class=\"overlay_item_name\">" + msgcat.getString("DIALOG_CREATE_NOTE_TAGS") + "</td>");
        {
            writer.print("<td>");
            writer.print("<input");
            writer.print(" id=\"noteDialogTags\"");
            writer.print(" form=\"makenote\"");
            writer.print(" type=\"text\"");
            writer.print(" name=\"tags\"");
            writer.print(" autocomplete=\"off\"");
            writer.print(" size=\"" + INPUT_LENGTH_CHARS + "\"");
            writer.print(" maxlength=\"" + Limit.NOTE_TAGS_LENGTH + "\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(">");
            writer.print("</td>");
            writer.println("");
        }
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.print("              <td>" + msgcat.getString("DIALOG_CREATE_BOOK_ICON") + "</td>");
        {
            writer.print("<td>");
            writer.print("<input");
            writer.print(" id=\"noteDialogImage\"");
            writer.print(" form=\"makenote\"");
            writer.print(" type=\"file\"");
            writer.print(" name=\"image\"");
            writer.print(" accept=\".jpg,.png\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(">");
            writer.print("</td>");
            writer.println("");
        }
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.print("              <td colspan=\"2\">");
        writer.print("<span id=\"noteDialogErrorOutput\" class=\"error\"></span>");
        writer.println("</td>");
        writer.println("        </tr>");

        writer.println("        <tr>");
        writer.println("            <td>");
        writer.println("            </td>");
        writer.print("            <td align=\"center\">");
        {
            writer.print("<button");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"click_button_create_note_proceed(" + bookId + ")\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(">");
            writer.print(msgcat.getString("CONFIRM"));
            writer.println("</button>");
        }
        {
            writer.print("<button");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"generic.overlayClose()\"");
            writer.print(" tabindex=\"" + (tabindex++) + "\"");
            writer.print(" onfocus=\"this.tabIndex=1\"");
            writer.print(" onblur=\"this.tabIndex=" + (tabindex - 2) + "\"");
            writer.print(">");
            writer.print(msgcat.getString("CANCEL"));
            writer.println("</button>");
        }
        writer.println("         </td>");
        writer.println("            </tr>");

        writer.println("    </table>");
        writer.println("</div>");
    }
    /*
     *
     */
    private void deleteNoteDialog(HttpSession session, PrintWriter writer, MyNotes.MsgCat msgcat) {
        writer.println("<div id=\"noteDeleteDialog\" class=\"popup overlay_dialog with_border\" onclick=\"stop_propagation(event)\">");

        writer.println("    <table>");

        writer.println("        <tr>");
        writer.print("              <td colspan=\"2\">");
        writer.print("<span id=\"noteDeleteDialogErrorOutput\" class=\"error\"></span>");
        writer.println("</td>");
        writer.println("        </tr>");

        writer.println("            <tr>");
        writer.print("                <td align=\"center\">");
        {
            writer.print("<button");
            writer.print(" id=\"noteDeleteDialogButton\"");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"click_button_delete_note_proceed()\"");
            writer.print(" tabindex=\"2\"");
            writer.print(">");
            writer.print(msgcat.getString("CONFIRM"));
            writer.println("</button>");
        }
        writer.println("</td>");
        writer.print("                <td align=\"center\">");
        {
            writer.print("<button");
            writer.print(" class=\"button button_dark\"");
            writer.print(" onclick=\"generic.overlayClose()\"");
            writer.print(" tabindex=\"3\"");
            writer.print(" onfocus=\"this.tabIndex=1\"");
            writer.print(" onblur=\"this.tabIndex=2\"");
            writer.print(">");
            writer.print(msgcat.getString("CANCEL"));
            writer.println("</button>");
        }
        writer.println("</td>");
        writer.println("            </tr>");

        writer.println("    </table>");
        writer.println("</div>");
    }

    /*
     *
     */
    public static User getUserFromSession(HttpSession session) throws SQLException, NoSuchUserException {
        String userName = (String)session.getAttribute("user");
        if (userName == null)
            return null;

        User user = null;

        try {
            user = User.loadByName(userName);
        } catch (SQLException e) {
            MyNotes.log(Level.SEVERE, "Failed to load user from session, \"" + userName + "\", " + e);
            throw e;
        } catch (NoSuchUserException e) {
            MyNotes.log(Level.SEVERE, "Failed to find user from session attribute, \"" + userName + "\"");
            throw e;
        } finally {
            if (user == null)
                session.removeAttribute("user");
        }

        return user;
    }
    /*
     *
     */
    public static Long getLongParameter(HttpServletRequest request, String name) {
        String stringValue = request.getParameter(name);

        if (stringValue == null)
            return null;

        Long longValue;
        try {
            longValue = new Long(stringValue);
        } catch (NumberFormatException e) {
            return null;
        }

        return longValue;
    }
    /*
     *
     */
    public static UserState loadUserState(HttpSession session) {
        UserState userState = (UserState)session.getAttribute("userState");
        if (userState == null) {
            userState = new UserState();
            session.setAttribute("userState", userState);
        }
        return userState;
    }
}


