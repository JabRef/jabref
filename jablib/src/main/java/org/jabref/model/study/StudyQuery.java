package org.jabref.model.study;

import java.util.Objects;

public class StudyQuery {
    private String query;

    public StudyQuery(String query) {
        this.query = query != null ? query : "";
    }

    /// Used for Jackson deserialization
    public StudyQuery() {
        this.query = "";
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StudyQuery that = (StudyQuery) o;
        return Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query);
    }

    @Override
    public String toString() {
        return "QueryEntry{" +
                "query='" + query + '\'' +
                '}';
    }
}
