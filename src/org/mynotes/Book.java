/*
 *
 */
package org.mynotes;

import java.util.logging.Level;
import java.util.ArrayList;

import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.mynotes.User;
import org.mynotes.MyNotes;

/*
 *
 */
public class Book {
    public Long id; 
    public User user;
    public String access;
    public String name; 
    public String description;
    public String icon;
    public Tags tags; /* TODO save/restore from db */

    public Book() {
        tags = new Tags();
    }

    /**
     * Load all books from DB to which supplied user have access to.
     */
    public static ArrayList<Book> load(User user) throws SQLException {
        return load(user, null, null);
    }
    /**
     * Load all books from DB to which supplied user have access to.
     */
    public static ArrayList<Book> load(User user, BookFilter filter) throws SQLException {
        return load(user, null, filter);
    }
    /*
     *
     */
    public static ArrayList<Book> load(User user, Long bookId, BookFilter filter) throws SQLException {
        ArrayList<Book> books = new ArrayList<>();

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        StringBuilder query = null;
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();

                if (true) {
                    query = new StringBuilder();
                    query.append("SELECT * FROM");

                    /*
                     * Apply tag filter if necessary
                     */
                    if (filter != null && filter.tagFilter.get().size() > 0) {
                        mkQueryTagFilter(user, query, filter.tagFilter.get());
                    } else {
                        query.append(" access");
                    }

                    query.append(" WHERE userid = " + user.id);
                    if (bookId != null)
                        query.append(" and bookid = " + bookId);
                    query.append(" ORDER BY bookid");

                    ResultSet rs = statement.executeQuery(query.toString());
                    while(rs.next())
                    {
                        Book book   = new Book();
                        book.id     = rs.getLong("bookid");
                        book.user   = user;
                        book.access = rs.getString("value");
                        books.add(book);
                    }
                }

                for (Book book: books) {
                    query = new StringBuilder();
                    query.append("SELECT * FROM books");
                    query.append(" WHERE id = " + book.id);
                    ResultSet rs = statement.executeQuery(query.toString());
                    while(rs.next())
                    {
                        book.name        = rs.getString("name");
                        book.description = rs.getString("description");
                        book.icon        = rs.getString("icon");
                    }
                    book.loadTags(statement);
                }
            } catch (SQLException e) {
                throw new SQLException(e + ", QUERY: " + query);
            }
        }

        return books;
    }
    /**
     * Load specific book for user.
     */
    public static Book loadForUser(User user, Long bookId) throws SQLException, Exception {
        Book book;
        ArrayList<Book> books = Book.load(user, bookId, null);
        if (books.size() != 1)
            throw new Exception("not single/no access");
        book = books.get(0);
        return book;
    }
    /*
     *
     */
    private void loadTags(Statement statement) throws SQLException {
        if (true) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM booktags");
            query.append(" WHERE bookid = " + id);

            ResultSet rs = statement.executeQuery(query.toString());
            while(rs.next())
                tags.add(rs.getString("value"));
        }
    }
    /*
     *
     */
    private static void mkQueryTagFilter(User user, StringBuilder query,
            ArrayList<String> filter) {
        query.append(" (SELECT * FROM");
        if (filter.size() > 1)
            mkQueryTagFilter(user, query, new ArrayList<>(filter.subList(1, filter.size())));
        else
            query.append(" access");
        query.append(" WHERE userid = " + user.id);
        query.append(" AND bookid IN ");
        query.append("(SELECT bookid FROM booktags WHERE value = '" + filter.get(0) + "'))");
    }

    /**
     * Get author of this book.
     */
    public User getAuthor() throws SQLException, Exception {
        User user = null;

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        synchronized (connection) {
            try {
                Long userid = null;

                Statement statement = connection.createStatement();
                if (true) {
                    StringBuilder query = new StringBuilder();
                    query.append("SELECT * FROM access");
                    query.append(" WHERE bookid = " + this.id);
                    ResultSet rs = statement.executeQuery(query.toString());
                    while(rs.next())
                    {
                        if (!rs.getString("value").equals("A"))
                            continue;
                        userid = rs.getLong("userid");
                        break;
                    }
                }
                if (userid != null) {
                    user = User.loadById(userid);
                }
            } catch (SQLException e) {
                throw e;
            }
        }

        if (user == null)
            throw new Exception("Failed to get author for book " + this.id);

        return user;
    }

    /*
     *
     */
    public void storeNew() throws SQLException {
        store(true);
    }
    /*
     *
     */
    public void store() throws SQLException {
        store(false);
    }
    /*
     *
     */
    public void store(boolean newBook) throws SQLException {
        Connection connection = MyNotes.getInstance().getUserDbConnection();
        StringBuilder query = null;
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();

                query = new StringBuilder();
                if (newBook) {
                    query.append("INSERT INTO books");
                    query.append(" (");
                    query.append(" name, description) ");
                    query.append(" VALUES(");
                    query.append("'" + MyUtil.SQLEscape(name)        + "',"); /* XXX escaping not tested well */
                    query.append("'" + MyUtil.SQLEscape(description) + "'"); /* XXX escaping not tested well */
                    query.append(")");
                    statement.executeUpdate(query.toString());

                    ResultSet rs = statement.executeQuery("select last_insert_rowid()");
                    while(rs.next())
                    {
                        this.id = rs.getLong("last_insert_rowid()");
                        break;
                    }
                } else {
                    query.append("UPDATE books SET");
                    query.append(" name = '" + MyUtil.SQLEscape(name)        + "',"); /* XXX escaping not tested well */
                    query.append(" description = '" + MyUtil.SQLEscape(description) + "'"); /* XXX escaping not tested well */
                    query.append(" WHERE id = " + id);
                    statement.executeUpdate(query.toString());
                }

                query = new StringBuilder();
                query.append("insert or replace into access");
                query.append(" (bookid, userid, value) ");
                query.append(" values(");
                query.append(id + ",");
                query.append(user.id + ",");
                query.append("'A'"); /* NOTE author uncoditionally */
                query.append(")");
                statement.executeUpdate(query.toString());

                /*
                 * Delete tags that not present
                 */
                query = new StringBuilder();
                query.append("DELETE from booktags WHERE");
                query.append(" bookid = " + id);
                if (tags.get().size() > 0) {
                    query.append(" AND value NOT IN (");

                    ArrayList<String> list = tags.get();
                    long tagsSize = list.size();

                    for (int i = 0; i < tagsSize; i++) {
                        query.append("'" + MyUtil.SQLEscape(list.get(i)) + "'");
                        if (i < (tagsSize - 1))
                            query.append(", ");
                    }
                    query.append(")");
                }
                statement.executeUpdate(query.toString());

                /*
                 * Store tags
                 */
                for (String tag: tags.get()) {
                    query = new StringBuilder();
                    query.append("insert or replace into booktags");
                    query.append(" values(");
                    query.append("" + id + ",");
                    query.append("'" + MyUtil.SQLEscape(tag) + "'");
                    query.append(")");
                    statement.executeUpdate(query.toString());
                }
            } catch (SQLException e) {
                throw new SQLException(e + ", QUERY: " + query);
            }
        }
    }
    /**
     * Store icon info.
     */
    public void storeIcon() throws SQLException {
        if (icon == null)
            return;

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        StringBuilder query = null;
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();

                query = new StringBuilder();
                query.append("UPDATE books");
                query.append(" SET icon = '" + MyUtil.SQLEscape(icon) + "'");
                query.append(" WHERE id = " + id);
                statement.executeUpdate(query.toString());
            } catch (SQLException e) {
                throw new SQLException(e + ", QUERY: " + query);
            }
        }
    }
    /**
     * Completely delete book from database (book, notes, access info).
     */
    public static void delete(Long bookId) throws SQLException {
        Connection connection = MyNotes.getInstance().getUserDbConnection();
        synchronized (connection) {
            try {
                StringBuilder query;
                Statement statement = connection.createStatement();

                query = new StringBuilder();
                query.append("delete from notes");
                query.append(" where ");
                query.append(" bookid = " + bookId);
                statement.executeUpdate(query.toString());

                query = new StringBuilder();
                query.append("delete from access");
                query.append(" where ");
                query.append(" bookid = " + bookId);
                statement.executeUpdate(query.toString());

                query = new StringBuilder();
                query.append("delete from booktags");
                query.append(" where ");
                query.append(" bookid = " + bookId);
                statement.executeUpdate(query.toString());

                query = new StringBuilder();
                query.append("delete from books");
                query.append(" where ");
                query.append(" id = " + bookId);
                statement.executeUpdate(query.toString());
            } catch (SQLException e) {
                throw e;
            }
        }
    }
}

