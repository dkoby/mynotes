/**
 *
 */
package org.mynotes;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

import org.mynotes.User;
import org.mynotes.Limit;
import org.mynotes.exceptions.NoSuchUserException;
import org.mynotes.exceptions.DuplicateUserException;
import org.mynotes.exceptions.InvalidUserConfirmString;

/**
 *
 */
public class MyNotes {
    private static MyNotes app = null;
    private static Logger logger;
    private File dataPath;
    private File dbPath;
    private File imgPath;
    public File bookIconPath;
    public File noteImagePath;
    private Connection dbConnection;
    private HashMap<String, User> newUsers;

    /*
     * 
     */
    public static void make() throws Exception {
        app = new MyNotes();

        app.logger   = Logger.getLogger(app.getClass().getSimpleName());
        app.newUsers = new HashMap<>();

//        if (true) {
//            final File tmp = new File(System.getProperty("java.io.tmpdir"));
//
//            MyNotes.log(Level.INFO, "TMP: " + tmp);
//            if (!tmp.exists() || !tmp.isDirectory() || !tmp.canRead() || !tmp.canWrite()) {
//                throw new Exception("error with tmpDir");
//            }            
//        }

        try {
            Class.forName("org.sqlite.JDBC");        
        } catch (Exception e) {
            app.logger.severe("failed to load SQLite JDBC, " + e);
            throw e;
        }

        app.makeDirs();
        try {
            app.dbConnection = DriverManager.getConnection("jdbc:sqlite:" + new File(app.dbPath, "main.db"));
            app.makeDbTables();
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {

        }
    } 
    /*
     *
     */
    public static MyNotes getInstance() {
        return app;
    }
    /**
     * Syntatic sugar for MsgCat class construct constructor.
     */
    public MsgCat getMsgCat(Locale locale) {
        return new MsgCat(locale);
    }
    /**
     * Class for localization messages. Should not used directly.
     * Use MyNotes.getMsgCat() instead.
     */
    public class MsgCat {
        private ResourceBundle bundle;
        /**
         *
         */
        public MsgCat(Locale locale) {
            try {
                bundle = ResourceBundle.getBundle("msgcat/msgcat", locale);
            } catch (Exception e) {
                bundle = ResourceBundle.getBundle("msgcat/msgcat", new Locale("en"));
            }
        }
        public String getString(String handle) {
            try {
                return bundle.getString(handle);
            } catch (MissingResourceException e) {
                return "???";
            }
        }
    }
    /**
     * Terminate application.
     */
    public void terminate() {
        /* XXX is synchronized not necessary? */
        synchronized (newUsers) {
            for (User user: newUsers.values()) {
                user.terminate();
            }
        }
        try {
            if(dbConnection != null)
                dbConnection.close();
        } catch(SQLException e) {
            app.logger.severe("Failed to close data base connection");
        }
    }
    /*
     *
     */
    public static void makeDir(File dir) throws Exception {
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new Exception("Failed to create \"" + dir.toString() + "\", " +
                        "regular file already exists with such name");
            }
        } else {
            if (!dir.mkdirs()) {
                throw new Exception("Failed to create \"" + dir.toString() + "\" directory");
            }
        }
    }

    /**
     * Create necessary directories used by program.
     */
    private void makeDirs() throws Exception {
        Map<String, String> env = System.getenv();

        String value = env.get("CATALINA_BASE");
        if (value == null) {
            throw new Exception("Failed to get CATALINA_BASE environment variable");
        }

        File catalinaBase = new File(value);

        dataPath      = new File(catalinaBase, "data");
        dbPath        = new File(dataPath    , "db");
        imgPath       = new File(dataPath    , "img"); 
        bookIconPath  = new File(imgPath     , "book");
        noteImagePath = new File(imgPath     , "note");

        makeDir(dataPath);
        makeDir(dbPath);
        makeDir(imgPath);
        makeDir(bookIconPath);
        makeDir(noteImagePath);
    }    
    /**
     * 
     */
    private void makeDbTables() throws SQLException {
        Statement statement = dbConnection.createStatement();
        StringBuilder query;
        query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS users");
        query.append("(");
        query.append("id INTEGER PRIMARY KEY AUTOINCREMENT,");
        query.append("name TEXT (" + Limit.USER_NAME_LENGTH + ") NOT NULL UNIQUE,");
        query.append("password TEXT (" + Limit.USER_PASSWORD_LENGTH + ") NOT NULL,");
        query.append("email TEXT (" + Limit.USER_EMAIL_LENGTH + ") NOT NULL");
        query.append(")");
        statement.executeUpdate(query.toString());

        query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS books");
        query.append("(");
        query.append("id INTEGER PRIMARY KEY AUTOINCREMENT,");
        query.append("name TEXT (" + Limit.BOOK_NAME_LENGTH + ") NOT NULL,");
        query.append("description TEXT (" + Limit.BOOK_DESCRIPTION_LENGTH + "),");
        query.append("icon TEXT (" + Limit.BOOK_ICON_NAME_LENGTH + ")");
        query.append(")");
        statement.executeUpdate(query.toString());

        query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS access");
        query.append("(");
        query.append("bookid REFERENCES book (id),");
        query.append("userid REFERENCES users (id),");
        query.append("value TEXT (1) NOT NULL,");
        query.append("PRIMARY KEY (bookid, userid)");
        query.append(")");
        statement.executeUpdate(query.toString());

        query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS notes");
        query.append("(");
        query.append("id INTEGER PRIMARY KEY AUTOINCREMENT,");
        query.append("bookid REFERENCES books (id) NOT NULL,");
        query.append("authorid REFERENCES users (id) NOT NULL,");
        query.append("lastedituserid REFERENCES users (id),");
        query.append("creationtime INTEGER NOT NULL,");
        query.append("edittime INTEGER NOT NULL,");
        query.append("icon TEXT,");
        query.append("image TEXT,");
        query.append("color TEXT,");
        query.append("message TEXT (" + Limit.NOTE_MESSAGE_LENGTH + ") NOT NULL");
        query.append(")");
        statement.executeUpdate(query.toString());

        query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS notetags");
        query.append("(");
        query.append("noteid REFERENCES notes (id),");
        query.append("value TEXT (" + Limit.NOTE_TAG_LENGTH + ")  NOT NULL,");
        query.append("PRIMARY KEY (noteid, value)");
        query.append(")");
        statement.executeUpdate(query.toString());

        query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS booktags");
        query.append("(");
        query.append("bookid REFERENCES books (id),");
        query.append("value TEXT (" + Limit.BOOK_TAG_LENGTH + ")  NOT NULL,");
        query.append("PRIMARY KEY (bookid, value)");
        query.append(")");
        statement.executeUpdate(query.toString());

    }
    /*
     *
     */
    public Connection getUserDbConnection() {
        return dbConnection;
    }
    /**
     * Make new, not confirmed user.
     */
    public void newUser(String name, String password, String email) throws DuplicateUserException, SQLException {
        try {
            User.loadByName(name);
        } catch (SQLException e) {
            throw e;
        } catch (NoSuchUserException e) {
            synchronized (newUsers) {
                newUsers.put(name, User.makeNew(name, password, email));
            }
            return;
        }
        throw new DuplicateUserException(name);
    }
    public Map<String, User> getNewUsers() {
        return newUsers;
    }
    public void removeNewUser(String name) {
        synchronized (newUsers) {
            newUsers.remove(name);
        }
    }
    public synchronized void confirmUser(String name, String confirm) throws NoSuchUserException, InvalidUserConfirmString, SQLException {
        User user = newUsers.get(name);
        if (user == null)
            throw new NoSuchUserException(name);
        if (!user.confirm.equals(confirm))
            throw new InvalidUserConfirmString(name);
        removeNewUser(name);
        user.store();
    }

    /*
     *
     */
    public Logger getLogger() {
        return logger;
    }
    /*
     *
     */
    public static void log(Level level, String message) {
        synchronized (logger) {
            logger.log(level, message);
        }
    }
//    /*
//     *
//     */
//    public static void log(Logger logger, Level level, String message) {
//        synchronized (logger) {
//            logger.log(level, message);
//        }
//    }

}

