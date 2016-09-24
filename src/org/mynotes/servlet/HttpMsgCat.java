/*
 *
 */
package org.mynotes.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mynotes.MyNotes;

/**
 * This servlet used by javascript to obtain localized messages from server.
 */
public final class HttpMsgCat extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        MyNotes.MsgCat msgcat = MyNotes.getInstance().getMsgCat(request.getLocale());

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        String msg = request.getParameter("message");

        /* TODO additional parameters and printf like substitution */

        try (PrintWriter writer = response.getWriter()) {
            if (msg == null)
                writer.print("???");
            else
                writer.print(msgcat.getString(msg));
        }
    }
}

