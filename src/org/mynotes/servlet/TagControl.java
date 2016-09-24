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

//import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mynotes.UserState;
import org.mynotes.Tags;

/**
 * Servlet for add/remove tags for filter.
 */
public final class TagControl extends HttpServlet {
    /*
     *
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        UserState userState = Home.loadUserState(request.getSession());

        String tagType = request.getParameter("type");
        if (tagType == null) {
            throw new ServletException("No type specified");
        }

        Tags tags;
        switch (tagType) {
            case "notetag":
                tags = userState.noteTags;
                break;
            case "booktag":
                tags = userState.bookTags;
                break;
            default:
                throw new ServletException("Unknown tag type");
        }

        String clear = request.getParameter("clear");
        if (clear != null) {
            synchronized (userState) {
                tags.clear();
            }
            return;
        }

        String tag = request.getParameter("tag");
        if (tag == null)
            throw new ServletException("No tag specified");

        /* NOTE append # to tag */
        tag = "#" + tag;

        String add = request.getParameter("add");
        if (add != null) {
            synchronized (userState) {
                tags.add(tag);
            }
            return;
        }

        String remove = request.getParameter("remove");
        if (remove != null) {
            synchronized (userState) {
                tags.remove(tag);
            }
            return;
        }

        throw new ServletException("Unknown action");
    }
}

