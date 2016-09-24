/*
 *
 */
package org.mynotes;

public class NoteFilter {
    public Tags tagFilter;
    public Long pageNum;
    public long notesPerPage;
    public PageInfo pageInfo;

    public NoteFilter(Long pageNum, long notesPerPage) {
        this.pageNum = pageNum;
        this.notesPerPage = notesPerPage;
        tagFilter = new Tags();
        pageInfo = new PageInfo();
    }

    /*
     *
     */
    public class PageInfo {
        public long page;
        public long pages;
    }
}

