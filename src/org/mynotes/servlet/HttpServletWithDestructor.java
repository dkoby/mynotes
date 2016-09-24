/*
 *
 */
package org.mynotes.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
abstract class HttpServletWithDestructor extends HttpServlet {
    private boolean shutDown = false;
    private long serviceCounter = 0;

    /*
     *
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        synchronized (this) {
            serviceCounter++;
        }
        try {
            super.service(request, response);
        } finally {
            synchronized (this) {
                serviceCounter--;
            }
        }
    }
    /*
     *
     */
    private synchronized void setShutDown() {
        shutDown = true;
    }
    /*
     *
     */
    private synchronized long numServices() {
        return serviceCounter;
    }
    /**
     *
     */
    @Override
    public void destroy() {
        log("Destroy, " + this);
        setShutDown();
        while (numServices() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log("interrupted" + this);
                /* XXX break here? */
            }
        }
        onDestroy();
    }
    /**
     *
     */
    abstract public void onDestroy();
    /*
     *
     */
    private synchronized boolean isShutDown() {
        return shutDown;
    }
}


