package org.jabref.model.study;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * This class represents a scientific study.
 *
 * This class defines all aspects of a scientific study relevant to the application. It is a proxy for the file based study definition.
 */
@JsonPropertyOrder({"authors", "title", "research-questions", "queries", "databases"})
// The user might add arbitrary content to the YAML
@JsonIgnoreProperties(ignoreUnknown = true)
public class Study {
    private List<String> authors;

    private String title;

    @JsonProperty("research-questions")
    private List<String> researchQuestions;

    private List<StudyQuery> queries;

    private List<StudyDatabase> databases;

    public Study(List<String> authors, String title, List<String> researchQuestions, List<StudyQuery> queryEntries, List<StudyDatabase> databases) {
        this.authors = authors;
        this.title = title;
        this.researchQuestions = researchQuestions;
        this.queries = queryEntries;
        this.databases = databases;
    }

    /**
     * Used for Jackson deserialization
     */
    public Study() {
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public List<StudyQuery> getQueries() {
        return queries;
    }

    public void setQueries(List<StudyQuery> queries) {
        this.queries = queries;
    }

    public List<StudyDatabase> getDatabases() {
        return databases;
    }

    public void setDatabases(List<StudyDatabase> databases) {
        this.databases = databases;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getResearchQuestions() {
        return researchQuestions;
    }

    public void setResearchQuestions(List<String> researchQuestions) {
        this.researchQuestions = researchQuestions;
    }

    @Override
    public String toString() {
        return "Study{" +
                "authors=" + authors +
                ", studyName='" + title + '\'' +
                ", researchQuestions=" + researchQuestions +
                ", queries=" + queries +
                ", libraries=" + databases +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Study otherStudy = (Study) other;

        return Objects.equals(authors, otherStudy.authors) &&
                Objects.equals(title, otherStudy.title) &&
                Objects.equals(researchQuestions, otherStudy.researchQuestions) &&
                Objects.equals(queries, otherStudy.queries) &&
                Objects.equals(databases, otherStudy.databases);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this);
    }
}

