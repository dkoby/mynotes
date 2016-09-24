/*
 *
 */
package tasks;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;

import java.util.Base64;

import java.lang.reflect.Method;

/**
 *
 */
public class TomcatTool extends Task {
    private String action;
    private String war;
    private String managerUrl;
    private String managerUser;
    private String managerPassword;
    private String managerPath;
    private Boolean managerUpdate;
    
    /**
     *
     */
    public void setAction(String action) {
//        switch (action) {
//            case "deploywar":
//            case "reload":
//                break;
//            default:
//                throw new BuildException("Action \"" + action + "\" not supported");
//        }
        this.action = action;
    }
    /**
     *
     */
    public void setWar(String war) {
        this.war = war;
    }
    /**
     *
     */
    public void setManagerurl(String managerUrl) {
        this.managerUrl = managerUrl;
    }
    /**
     *
     */
    public void setManageruser(String managerUser) {
        this.managerUser = managerUser;
    }
    /**
     *
     */
    public void setManagerpassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }
    /**
     *
     */
    public void setManagerpath(String managerPath) {
        this.managerPath = managerPath;
    }
    /**
     *
     */
    public void setManagerupdate(Boolean managerUpdate) {
        this.managerUpdate = managerUpdate;
    }

    /**
     *
     */
    @Override
    public void execute() {
        if (action == null)
            throw new BuildException("No \"action\" attribute specified");

        AllowedActions actions = new AllowedActions();

        Method method;
        try {
            Class<?> c = actions.getClass();
            method = c.getMethod(action);
        } catch (Exception e) {
            throw new BuildException("Action \"" + action + "\" not supported, " + e);
        }

        try {
            method.invoke(actions);
        } catch (Exception e) {
            throw new BuildException(e.getCause());
        }
    }
    /*
     *
     */
    private void checkArgs(boolean checkPath) throws BuildException {
        if (managerUrl == null)
            throw new BuildException("No \"managerUrl\" attribute specified for this action");
        if (managerUser == null)
            throw new BuildException("No \"managerUser\" attribute specified for this action");
        if (managerPassword == null)
            throw new BuildException("No \"managerPassword\" attribute specified for this action");
        if (checkPath && managerPath == null)
            throw new BuildException("No \"managerPath\" attribute specified for this action");
    }
    /**
     *
     */
    private void httpExec(String url, FileInputStream fileInputStream) throws BuildException {
        try {
            log("URL          : " + url);
            log("--------------------");

            HttpURLConnection hconn = (HttpURLConnection)(new URL(url).openConnection());

            long contentLength = -1;
            if (fileInputStream != null) {
                contentLength = fileInputStream.getChannel().size();

                log("Content-Length: " + contentLength);
                hconn.setRequestProperty("Content-Type", "application/octet-stream");
                hconn.setRequestProperty("Content-Length", "" + contentLength);
                hconn.setFixedLengthStreamingMode(contentLength);

                hconn.setDoOutput(true);
                hconn.setRequestMethod("PUT");
            } else {
                hconn.setRequestMethod("GET");
            }
            hconn.setDoInput(true);  /* we should get response from Tomcat */
            hconn.setAllowUserInteraction(false);
            hconn.setUseCaches(false);
            hconn.setRequestProperty("User-Agent", "TomcatTool/1.0");

            Base64.Encoder encoder = Base64.getMimeEncoder();
            hconn.setRequestProperty("Authorization",
                                     "Basic " + encoder.encodeToString(
                                         (managerUser + ":" + managerPassword).getBytes())
                                     );
            hconn.connect();

            /* write war file */
            if (contentLength >= 0)
            {
                BufferedInputStream warStream = new BufferedInputStream(fileInputStream);

                final int BUFFER_SIZE = 1024;
                byte[] buf = new byte[BUFFER_SIZE];

                try (BufferedOutputStream out = new BufferedOutputStream(
                            hconn.getOutputStream(), BUFFER_SIZE)) {
                    while (true) {
                        int rd = warStream.read(buf, 0, BUFFER_SIZE);
                        if (rd < 0)
                            break;
                        out.write(buf, 0, rd); 
                    }
                    out.flush(); 
                } catch (Exception e) {
                    throw new BuildException(e);
                }
            }

            int responseCode = hconn.getResponseCode();
            if (responseCode < 0 || responseCode != HttpURLConnection.HTTP_OK)
                throw new BuildException(responseCode + " " + hconn.getResponseMessage());

            /* read response */
            try (BufferedReader hconReader = new BufferedReader(
                        new InputStreamReader(hconn.getInputStream()))) {
                int nline = 0;
                while (true) {
                    String line = hconReader.readLine();
                    if (line == null)
                        break;
                    log("> " + line);
                    if (nline++ == 0 && !line.matches("^OK.*"))
                        throw new BuildException(line);
                }
            } catch (Exception e) {
                throw new BuildException(e);
            }

            hconn.disconnect();
        } catch (Exception e) {
            throw new BuildException(e.getMessage());
        } finally {
            try {
                if (fileInputStream != null)
                    fileInputStream.close();
            } catch (Exception e) {
                /* Ignore */
            }
        }
    }
    /*
     *
     */
    private class AllowedActions {
        /*
         *
         */
        public void deploywar() throws BuildException {
            if (war == null)
                throw new BuildException("No \"war\" attribute specified for this action");
            checkArgs(true);

            log("------ Deploy ------");
            log("war          : " + war);
            log("path         : " + managerPath);

            StringBuilder request = new StringBuilder(managerUrl + "/deploy?path=" + managerPath);
            if (managerUpdate)
                request.append("&update=true");

    //        BufferedInputStream warStream = null;
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(war);
            } catch (Exception e) {
                throw new BuildException(e);
            }
            httpExec(request.toString(), fileInputStream);
        }
        /**
         *
         */
        public void undeploy() throws BuildException {
            checkArgs(true);

            log("------ Undeploy ------");
            log("path         : " + managerPath);

            httpExec(managerUrl + "/undeploy?path=" + managerPath, null);
        }
        /**
         *
         */
        public void reload() throws BuildException {
            checkArgs(true);

            log("------ Reload ------");
            log("path         : " + managerPath);

            httpExec(managerUrl + "/reload?path=" + managerPath, null);
        }
        /**
         *
         */
        public void start() throws BuildException {
            checkArgs(true);

            log("------ Start ------");
            log("path         : " + managerPath);

            httpExec(managerUrl + "/start?path=" + managerPath, null);
        }
        /**
         *
         */
        public void stop() throws BuildException {
            checkArgs(true);

            log("------ Stop ------");
            log("path         : " + managerPath);

            httpExec(managerUrl + "/stop?path=" + managerPath, null);
        }
        /**
         *
         */
        public void list() throws BuildException {
            checkArgs(false);

            log("------ List --------");
            httpExec(managerUrl + "/list", null);
        }
    }
}

