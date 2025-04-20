package org.jabref.model.paging;

import java.util.Collection;
import java.util.Collections;

public class Page<T> {

    private int pageNumber;
    private String query;
    private Collection<T> content;

    public Page(String query, int pageNumber, Collection<T> content) {
        this.query = query;
        this.pageNumber = pageNumber;
        this.content = Collections.unmodifiableCollection(content);
    }

    public Page(String query, int pageNumber) {
        this(query, pageNumber, Collections.emptyList());
    }

    public Collection<T> getContent() {
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
