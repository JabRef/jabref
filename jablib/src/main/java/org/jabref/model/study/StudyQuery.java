package org.jabref.model.study;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StudyQuery {
    private String query;
    private String description;
    private String lucene;

    @JsonProperty("catalogue-specific")
    private Map<String, String> catalogueSpecific;

    public StudyQuery(String query) {
        this.query = query;
    }

    // Constructor for enhanced query with all fields
    public StudyQuery(String query, String description, String lucene, Map<String, String> catalogueSpecific) {
        this.query = query;
        this.description = description;
        this.lucene = lucene;
        this.catalogueSpecific = catalogueSpecific;
    }

    /**
     * Used for Jackson deserialization
     */
    public StudyQuery() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLucene() {
        return lucene;
    }

    public void setLucene(String lucene) {
        this.lucene = lucene;
    }

    public Map<String, String> getCatalogueSpecific() {
        return catalogueSpecific;
    }

    public void setCatalogueSpecific(Map<String, String> catalogueSpecific) {
        this.catalogueSpecific = catalogueSpecific;
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
