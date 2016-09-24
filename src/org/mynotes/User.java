/*
 *
 */
package org.mynotes;

import javax.servlet.ServletContext;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;
import java.security.MessageDigest;
import java.nio.ByteBuffer;

import java.sql.Connection;
//import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.mynotes.MyNotes;
import org.mynotes.MyUtil;
import org.mynotes.exceptions.NoSuchUserException;

/*
 *
 */
public class User {
    public String name;
    public Long id;
    public String password;
    public String email;
    public String confirm;
    private Thread removeThread;
    /*
     *
     */
    private User() {
    }
    /*
     *
     */
    private User(String name) {
        super();
        this.name = name;
    }
    /**
     *
     */
    public static User makeNew(String name, String password, String email) {
        User user = new User(name);

        user.password = password;
        user.email    = email;
        user.confirm  = User.makeConfirm(name);

        user.removeThread = new Thread() {
            private final long CONFIRM_TIMEOUT = (2 * 60 * 60 * 1000); /* 2 hours */
            @Override
            public void run() {
                try {
                    Thread.sleep(CONFIRM_TIMEOUT);
                    MyNotes.log(Level.INFO, "confirmation time expired, remove new user \"" + user.name + "\"");
                    MyNotes.getInstance().removeNewUser(user.name);
                } catch (InterruptedException e) {
                    MyNotes.log(Level.INFO, "interrupted (" + user.name + ")");
                } catch (Exception e) {
                    MyNotes.log(Level.INFO, "exception (" + user.name + "), " + e);
                }
            }
        };

        user.removeThread.setDaemon(true);
        user.removeThread.start();

        return user;
    }
    /**
     * Store user to databse.
     */
    public void store() throws SQLException {
        Connection connection = MyNotes.getInstance().getUserDbConnection();

        MyNotes.getInstance().getLogger().info("store user to DB \"" + name + "\"");

        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();
                StringBuilder query = new StringBuilder();

                query.append("insert into users");
                query.append(" (name, password, email) ");
                query.append(" values(");
                query.append("'" + MyUtil.SQLEscape(name)     + "',"); /* XXX escaping not tested well */
                query.append("'" + MyUtil.SQLEscape(password) + "',"); /* XXX escaping not tested well */
                query.append("'" + MyUtil.SQLEscape(email)    + "'");  /* XXX escaping not tested well */
                query.append(")");
                statement.executeUpdate(query.toString());
            } catch (SQLException e) {
                MyNotes.getInstance().getLogger().severe("failed to save user \"" + name + "\", " + e);
                throw e;
            }
        }
    }
    /**
     * Load user from database by name.
     */
    public static User loadByName(String name) throws NoSuchUserException, SQLException {
        User user = null;

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();
                StringBuilder query = new StringBuilder();

                query.append("select * from users");
                query.append(" where name = '" + MyUtil.SQLEscape(name) + "'"); /* XXX escaping not tested well */

                ResultSet rs = statement.executeQuery(query.toString());
                while(rs.next())
                {
                    user = new User(name).loadFields(rs);
                    break;
                }
            } catch (SQLException e) {
                MyNotes.log(Level.SEVERE, "failed to load user, " + name + ", " + e);
                throw e;
            }
        }

        if (user == null)
            throw new NoSuchUserException(name);
        return user;
    }
    /**
     * Load user from database by id.
     */
    public static User loadById(Long id) throws NoSuchUserException, SQLException {
        User user = null;

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();
                StringBuilder query = new StringBuilder();

                query.append("select * from users");
                query.append(" where id = " + id);

                ResultSet rs = statement.executeQuery(query.toString());
                while(rs.next())
                {
                    user = new User().loadFields(rs);
                    break;
                }
            } catch (SQLException e) {
                MyNotes.log(Level.SEVERE, "failed to load user, ID = " + id + ", " + e);
                throw e;
            }
        }

        if (user == null)
            throw new NoSuchUserException("ID = " + id);
        return user;
    }
    /*
     *
     */
    private User loadFields(ResultSet rs) throws SQLException {
        this.id       = rs.getLong("id");
        this.name     = rs.getString("name");
        this.password = rs.getString("password");
        this.email    = rs.getString("email");

        return this;
    }

    /**
     * Terminate unconfirmed user remove thread.
     */
    public void terminate() {
        if (removeThread != null)
            removeThread.interrupt();
    }
    /*
     *
     */
    private static String makeConfirm(String name) {
        StringBuilder resultHashString = new StringBuilder();
        int rand = (int)(Integer.MAX_VALUE * Math.random());
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            digest.update(name.getBytes("UTF-8"));
            digest.update(ByteBuffer.allocate(4).putInt(rand).array());

            for (byte b: digest.digest())
                resultHashString.append(String.format("%02X", b));
        } catch (Exception e) {
            for (byte b: name.getBytes())
                resultHashString.append(String.format("%02X", b));
            resultHashString = new StringBuilder(String.format("%08", rand));
        }

        return resultHashString.toString();
    }
}

