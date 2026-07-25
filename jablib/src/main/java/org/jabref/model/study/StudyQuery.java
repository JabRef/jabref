package org.jabref.model.study;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StudyQuery {
    private String query;

    @JsonProperty("catalog-specific")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> catalogSpecific;

    public StudyQuery(String query) {
        this.query = query != null ? query : "";
        this.catalogSpecific = new LinkedHashMap<>();
    }

    /// Used for Jackson deserialization
    public StudyQuery() {
        this.query = "";
        this.catalogSpecific = new LinkedHashMap<>();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, String> getCatalogSpecific() {
        return catalogSpecific;
    }

    public void setCatalogSpecific(Map<String, String> catalogSpecific) {
        this.catalogSpecific = catalogSpecific != null ? catalogSpecific : new LinkedHashMap<>();
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
        return Objects.equals(query, that.query) &&
                Objects.equals(catalogSpecific, that.catalogSpecific);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, catalogSpecific);
    }

    @Override
    public String toString() {
        return "QueryEntry{" +
                "query='" + query + '\'' +
                ", catalogSpecific=" + catalogSpecific +
                '}';
    }
}
