package org.jabref.model.study;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class StudyQuery {
    private String query;
    private Optional<String> description = Optional.empty();
    private Optional<String> lucene = Optional.empty();

    @JsonProperty("catalog-specific")
    private Optional<Map<String, String>> catalogSpecific = Optional.empty();

    public StudyQuery(String query) {
        this.query = query;
        this.description = Optional.empty();
        this.lucene = Optional.empty();
        this.catalogSpecific = Optional.empty();
    }

    public StudyQuery(String query, String description, String lucene, Map<String, String> catalogSpecific) {
        this.query = query;
        this.description = Optional.ofNullable(description);
        this.lucene = Optional.ofNullable(lucene);
        this.catalogSpecific = Optional.ofNullable(catalogSpecific);
    }

    /**
     * Used for Jackson deserialization
     */
    public StudyQuery() {
        this.description = Optional.empty();
        this.lucene = Optional.empty();
        this.catalogSpecific = Optional.empty();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Optional<String> getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(Optional<String> description) {
        this.description = description != null ? description : Optional.empty();
    }

    @JsonIgnore
    public void setDescription(String description) {
        this.description = Optional.ofNullable(description);
    }

    public Optional<String> getLucene() {
        return lucene;
    }

    @JsonSetter("lucene")
    public void setLucene(Optional<String> lucene) {
        this.lucene = lucene != null ? lucene : Optional.empty();
    }

    public Map<String, String> getCatalogSpecific() {
        return catalogSpecific.orElse(Map.of());
    }

    @JsonIgnore
    public Optional<Map<String, String>> getCatalogSpecificOptional() {
        return catalogSpecific;
    }

    @JsonSetter("catalog-specific")
    public void setCatalogSpecific(Optional<Map<String, String>> catalogSpecific) {
        this.catalogSpecific = catalogSpecific != null ? catalogSpecific : Optional.empty();
    }

    @JsonIgnore
    public void setCatalogSpecific(Map<String, String> catalogSpecific) {
        this.catalogSpecific = Optional.ofNullable(catalogSpecific);
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
                Objects.equals(description, that.description) &&
                Objects.equals(lucene, that.lucene) &&
                catalogSpecificEquals(that);
    }

    private boolean catalogSpecificEquals(StudyQuery that) {
        // Treat Optional.empty() and Optional.of(emptyMap) as equal
        Map<String, String> thisCatalogSpecific = this.catalogSpecific.orElse(Map.of());
        Map<String, String> thatCatalogSpecific = that.catalogSpecific.orElse(Map.of());
        return Objects.equals(thisCatalogSpecific, thatCatalogSpecific);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, description, lucene, catalogSpecific);
    }

    @Override
    public String toString() {
        return "QueryEntry{" +
                "query='" + query + '\'' +
                ", description=" + description +
                ", lucene=" + lucene +
                ", catalogSpecific=" + catalogSpecific +
                '}';
    }
}
