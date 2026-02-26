package org.jabref.model.study;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/// This class represents a scientific study.
///
/// This class defines all aspects of a scientific study relevant to the application. It is a proxy for the file based study definition.
///
/// The file is parsed using by {@link org.jabref.logic.crawler.StudyYamlParser}
@JsonPropertyOrder({"version", "authors", "title", "research-questions", "queries", "catalogs"})
// The user might add arbitrary content to the YAML
@JsonIgnoreProperties(ignoreUnknown = true)
public class Study {
    @JsonProperty("version")
    private String version;

    private List<String> authors;

    private String title;

    @JsonProperty("research-questions")
    private List<String> researchQuestions;

    private List<StudyQuery> queries;

    @JsonProperty("catalogs")
    private List<StudyCatalog> catalogs;

    public Study(List<String> authors, String title, List<String> researchQuestions, List<StudyQuery> queryEntries, List<StudyCatalog> catalogs) {
        this.authors = authors;
        this.title = title;
        this.researchQuestions = researchQuestions;
        this.queries = queryEntries;
        this.catalogs = catalogs;
    }

    /// Used for Jackson deserialization
    private Study() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public List<StudyCatalog> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(List<StudyCatalog> catalogs) {
        this.catalogs = catalogs;
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
                "version='" + version + '\'' +
                ", authors=" + authors +
                ", studyName='" + title + '\'' +
                ", researchQuestions=" + researchQuestions +
                ", queries=" + queries +
                ", catalogs=" + catalogs +
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

        return Objects.equals(version, otherStudy.version) &&
                Objects.equals(authors, otherStudy.authors) &&
                Objects.equals(title, otherStudy.title) &&
                Objects.equals(researchQuestions, otherStudy.researchQuestions) &&
                Objects.equals(queries, otherStudy.queries) &&
                Objects.equals(catalogs, otherStudy.catalogs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, authors, title, researchQuestions, queries, catalogs);
    }
}
