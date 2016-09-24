/*
 *
 */
package org.mynotes.servlet;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.annotation.WebListener;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.mynotes.MyNotes;

@WebListener()
public class MyWebListener implements ServletContextListener, HttpSessionListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            MyNotes.make();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Logger logger = MyNotes.getInstance().getLogger();
        synchronized (logger) {logger.info("Terminate");}

        MyNotes.getInstance().terminate();
    }
    @Override
    public void sessionCreated(HttpSessionEvent se) {

    }
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

    }
}

