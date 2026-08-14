package org.jabref.model.paging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Page<T> {

    private int pageNumber;
    private String query;
    private List<T> content;

    public Page(String query, int pageNumber, List<T> content) {
        this.query = query;
        this.pageNumber = pageNumber;
        this.content = Collections.unmodifiableList(new ArrayList<>(content));
    }

    public Page(String query, int pageNumber) {
        this(query, pageNumber, List.of());
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

    public int getSize() {
        return content.size();
    }
}
