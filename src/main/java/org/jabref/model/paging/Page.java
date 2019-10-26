package org.jabref.model.paging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Page<T> {

    private int pageNumber;
    private String query;
    private List<T> content;

    Page(int pageNumber, String query ,Collection<T> content) {
        this.pageNumber = pageNumber;
        this.query = query;
        this.content = new ArrayList<>(content);
    }

    public List<T> getContent() {
        return content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getQuery() {
        return query;
    }
}
