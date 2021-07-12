package org.jabref.model.study;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * This class represents a scientific study.
 *
 * This class defines all aspects of a scientific study relevant to the application. It is a proxy for the file based study definition.
 */

@JsonPropertyOrder({"authors", "title", "last-search-date", "research-questions", "queries", "databases"})
public class Study {
    private List<String> authors;
    private String title;
    @JsonProperty("last-search-date")
    private LocalDate lastSearchDate;
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

    public LocalDate getLastSearchDate() {
        return lastSearchDate;
    }

    public void setLastSearchDate(LocalDate date) {
        lastSearchDate = date;
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
                ", lastSearchDate=" + lastSearchDate +
                ", researchQuestions=" + researchQuestions +
                ", queries=" + queries +
                ", libraries=" + databases +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Study study = (Study) o;

        if (getAuthors() != null ? !getAuthors().equals(study.getAuthors()) : study.getAuthors() != null) {
            return false;
        }
        if (getTitle() != null ? !getTitle().equals(study.getTitle()) : study.getTitle() != null) {
            return false;
        }
        if (getLastSearchDate() != null ? !getLastSearchDate().equals(study.getLastSearchDate()) : study.getLastSearchDate() != null) {
            return false;
        }
        if (getResearchQuestions() != null ? !getResearchQuestions().equals(study.getResearchQuestions()) : study.getResearchQuestions() != null) {
            return false;
        }
        if (getQueries() != null ? !getQueries().equals(study.getQueries()) : study.getQueries() != null) {
            return false;
        }
        return getDatabases() != null ? getDatabases().equals(study.getDatabases()) : study.getDatabases() == null;
    }

    public boolean equalsBesideLastSearchDate(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Study study = (Study) o;

        if (getAuthors() != null ? !getAuthors().equals(study.getAuthors()) : study.getAuthors() != null) {
            return false;
        }
        if (getTitle() != null ? !getTitle().equals(study.getTitle()) : study.getTitle() != null) {
            return false;
        }
        if (getResearchQuestions() != null ? !getResearchQuestions().equals(study.getResearchQuestions()) : study.getResearchQuestions() != null) {
            return false;
        }
        if (getQueries() != null ? !getQueries().equals(study.getQueries()) : study.getQueries() != null) {
            return false;
        }
        return getDatabases() != null ? getDatabases().equals(study.getDatabases()) : study.getDatabases() == null;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this);
    }
}

