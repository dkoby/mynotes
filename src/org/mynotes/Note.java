/*
 *
 */
package org.mynotes;

import java.util.Date;
import java.util.logging.Level;
import java.util.ArrayList;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.mynotes.MyNotes;
import org.mynotes.NoteFilter;
import org.mynotes.Tags;

/*
 *
 */
public class Note {
    public Long id;
    public Long bookId;
    public Long authorId;
    public Long lastEditUserId;
    public Date creationTime;
    public Date editTime;
    public Tags tags;
    public String icon;
    public String image;
    public String color;
    public String message;

    /*
     *
     */
    public Note() {
        tags = new Tags();
    }

    /**
     *
     */
    public static ArrayList<Note> loadForBook(Book book, NoteFilter filter) throws SQLException {
        ArrayList<Note> notes = new ArrayList<>();

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        synchronized (connection) {
            StringBuilder query = null;
            try {
                Statement statement = connection.createStatement();
                ResultSet rs;

                if (true) {
                    query = new StringBuilder();
                    query.append("SELECT * FROM");

                    /*
                     * Apply tag filter if necessary
                     */
                    if (true) {
                        ArrayList<String> tagFilter = filter.tagFilter.get();
                        if (tagFilter.size() > 0) {
                            mkQueryTagFilter(book, query, tagFilter);
                        } else {
                            query.append(" notes WHERE bookid = " + book.id);
                        }
                    }

                    /*
                     * Get count of records
                     */
                    Long count = new Long(0);
                    rs = statement.executeQuery("SELECT count(*) FROM (" + query + ")");
                    while(rs.next()) {
                        count = rs.getLong("count(*)");
                        break;
                    }
//                    MyNotes.log(Level.INFO, "COUNT: " + count);

                    query.append(" ORDER BY creationtime");

                    /*
                     * Limit by page number
                     */
                    if (true) {
                        query.append(" LIMIT " + filter.notesPerPage);
                        filter.pageInfo.page = 0;
                        if (count > filter.notesPerPage) {
                            long offset = 0;
                            long pageCount = count / filter.notesPerPage + ((count % filter.notesPerPage) != 0 ? 1 : 0);
                            long pageNum;

                            if (filter.pageNum != null)
                                pageNum = filter.pageNum;
                            else
                                pageNum = pageCount - 1;

                            if (pageNum > pageCount - 1)
                                pageNum = pageCount - 1;

                            offset = pageNum * filter.notesPerPage;

                            query.append(" OFFSET " + offset);

//                            MyNotes.log(Level.INFO, "OFFSET: " + offset + ", PAGECOUNT " + pageCount);

                            filter.pageInfo.page = offset / filter.notesPerPage;
                        } 
                        filter.pageInfo.pages = count / filter.notesPerPage;
                        filter.pageInfo.pages += (count % filter.notesPerPage) > 0 ? 1 : 0;
                    }

                    /*
                     *
                     */
                    rs = statement.executeQuery(query.toString());
                    while(rs.next())
                        notes.add(new Note().loadFields(rs));
                }

                for (Note note: notes)
                    note.loadTags(statement);
            } catch (SQLException e) {
                throw new SQLException("" + e + ":: " + query);
            }
        }
        return notes;
    }
    /*
     *
     */
    private static void mkQueryTagFilter(Book book,
            StringBuilder query, ArrayList<String> filter) {
        query.append(" (SELECT * FROM");
        if (filter.size() > 1)
            mkQueryTagFilter(book, query, new ArrayList<>(filter.subList(1, filter.size())));
        else
            query.append(" notes");
        query.append(" WHERE bookid = " + book.id);
        query.append(" AND id IN ");
        query.append("(SELECT noteid FROM notetags WHERE value = '" + filter.get(0) + "'))");
    }

    /**
     *
     */
    public static Note loadById(Long id) throws SQLException, Exception {
        Note note = null;

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();

                if (true) {
                    StringBuilder query = new StringBuilder();
                    query.append("SELECT * FROM notes");
                    query.append(" WHERE id = " + id);

                    ResultSet rs = statement.executeQuery(query.toString());
                    while(rs.next())
                    {
                        note = new Note().loadFields(rs);
                        break;
                    }
                    if (note != null)
                        note.loadTags(statement);
                }

            } catch (SQLException e) {
                throw e;
            }
        }
        if (note == null)
            throw new Exception("No such note in database");
        return note;
    }
    /*
     *
     */
    private void loadTags(Statement statement) throws SQLException {
        if (true) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM notetags");
            query.append(" WHERE noteid = " + id);

            ResultSet rs = statement.executeQuery(query.toString());
            while(rs.next())
                tags.add(rs.getString("value"));
        }
    }
    /*
     *
     */
    private Note loadFields(ResultSet rs) throws SQLException {
        this.id             = rs.getLong("id");
        this.bookId         = rs.getLong("bookid");
        this.authorId       = rs.getLong("authorid");
        this.lastEditUserId = rs.getLong("lastedituserid");
        this.creationTime   = new Date(rs.getLong("creationtime") * 1000);
        this.editTime       = new Date(rs.getLong("edittime") * 1000);
        this.icon           = rs.getString("icon");
        this.image          = rs.getString("image");
        this.color          = rs.getString("color");
        this.message        = rs.getString("message");

        return this;
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
    /**
     * 
     */
    private void store(boolean newNote) throws SQLException {
        Connection connection = MyNotes.getInstance().getUserDbConnection();
        StringBuilder query = null;
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();

                query = new StringBuilder();
                if (newNote) {
                    query.append("INSERT INTO notes");
                    query.append(" (");
                    query.append(" bookid, authorid, lastedituserid, creationtime, edittime, icon, color, message) ");
                    query.append(" VALUES(");
                    query.append("" + bookId + ",");
                    query.append("" + authorId + ",");
                    query.append("" + lastEditUserId + ",");
                    query.append("" + (creationTime.getTime() / 1000) + ",");
                    query.append("" + (editTime.getTime() / 1000) + ",");
                    query.append("'" + MyUtil.SQLEscape(icon)  + "',"); /* XXX escaping not tested well */
                    query.append("'" + MyUtil.SQLEscape(color) + "',"); /* XXX escaping not tested well */
                    query.append("'" + MyUtil.SQLEscape(message) + "'");  /* XXX escaping not tested well */
                    query.append(")");
                    statement.executeUpdate(query.toString());

                    ResultSet rs = statement.executeQuery("select last_insert_rowid()");
                    while(rs.next())
                    {
                        this.id = rs.getLong("last_insert_rowid()");
                        break;
                    }
                } else {
                    query.append("UPDATE notes SET");
                    query.append(" bookid = " + bookId + ",");
                    query.append(" authorid = " + authorId + ",");
                    query.append(" lastedituserid = " + lastEditUserId + ",");
                    query.append(" creationtime = " + (creationTime.getTime() / 1000) + ",");
                    query.append(" edittime = " + (editTime.getTime() / 1000) + ",");
                    query.append(" icon = '" + MyUtil.SQLEscape(icon)  + "',"); /* XXX escaping not tested well */
                    query.append(" color = '" + MyUtil.SQLEscape(color) + "',"); /* XXX escaping not tested well */
                    query.append(" message = '" + MyUtil.SQLEscape(message) + "'");  /* XXX escaping not tested well */
                    query.append(" WHERE id = " + id);
                    statement.executeUpdate(query.toString());
                }

                /*
                 * Delete tags that not present.
                 */
                query = new StringBuilder();
                query.append("DELETE from notetags WHERE");
                query.append(" noteid = " + id);
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
                 * Store tags.
                 */
                for (String tag: tags.get()) {
                    query = new StringBuilder();
                    query.append("insert or replace into notetags");
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
    public void storeImage() throws SQLException {
        if (icon == null)
            return;

        Connection connection = MyNotes.getInstance().getUserDbConnection();
        StringBuilder query = null;
        synchronized (connection) {
            try {
                Statement statement = connection.createStatement();

                query = new StringBuilder();
                query.append("UPDATE notes");
                query.append(" SET image = '" + MyUtil.SQLEscape(image) + "'");
                query.append(" WHERE id = " + id);
                statement.executeUpdate(query.toString());
            } catch (SQLException e) {
                throw new SQLException(e + ", QUERY: " + query);
            }
        }
    }

    /**
     * Delete note from database.
     */
    public static void delete(Long noteId) throws SQLException {
        Connection connection = MyNotes.getInstance().getUserDbConnection();
        synchronized (connection) {
            try {
                StringBuilder query;
                Statement statement = connection.createStatement();

                query = new StringBuilder();
                query.append("delete from notetags");
                query.append(" where ");
                query.append(" noteid = " + noteId);
                statement.executeUpdate(query.toString());

                query = new StringBuilder();
                query.append("delete from notes");
                query.append(" where ");
                query.append(" id = " + noteId);
                statement.executeUpdate(query.toString());
            } catch (SQLException e) {
                throw e;
            }
        }
    }

}

