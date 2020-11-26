package org.jabref.model.study;

public class QueryEntry {
    private String query;

    public QueryEntry(String query) {
        this.query = query;
    }

    /**
     * Used for Jackson deserialization
     */
    public QueryEntry() {

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

        QueryEntry that = (QueryEntry) o;

        return getQuery() != null ? getQuery().equals(that.getQuery()) : that.getQuery() == null;
    }

    @Override
    public int hashCode() {
        return getQuery() != null ? getQuery().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "QueryEntry{" +
                "query='" + query + '\'' +
                '}';
    }
}
