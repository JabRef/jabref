package org.jabref.model.study;

import java.time.LocalDate;
import java.util.List;

/**
 * This class represents a scientific study.
 *
 * This class defines all aspects of a scientific study relevant to the application. It is a proxy for the file based study definition.
 */
public class Study {
    private List<String> authors;
    private String studyName;
    private LocalDate lastSearchDate;
    private List<String> researchQuestions;
    private List<QueryEntry> queries;
    private List<LibraryEntry> libraries;

    public Study(List<String> authors, String studyName, List<String> researchQuestions, List<QueryEntry> queryEntries, List<LibraryEntry> libraries) {
        this.authors = authors;
        this.studyName = studyName;
        this.researchQuestions = researchQuestions;
        this.queries = queryEntries;
        this.libraries = libraries;
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

    public List<QueryEntry> getQueries() {
        return queries;
    }

    public void setQueries(List<QueryEntry> queries) {
        this.queries = queries;
    }

    public LocalDate getLastSearchDate() {
        return lastSearchDate;
    }

    public void setLastSearchDate(LocalDate date) {
        lastSearchDate = date;
    }

    public List<LibraryEntry> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<LibraryEntry> libraries) {
        this.libraries = libraries;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
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
                ", studyName='" + studyName + '\'' +
                ", lastSearchDate=" + lastSearchDate +
                ", researchQuestions=" + researchQuestions +
                ", queries=" + queries +
                ", libraries=" + libraries +
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
        if (getStudyName() != null ? !getStudyName().equals(study.getStudyName()) : study.getStudyName() != null) {
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
        return getLibraries() != null ? getLibraries().equals(study.getLibraries()) : study.getLibraries() == null;
    }

    @Override
    public int hashCode() {
        int result = getAuthors() != null ? getAuthors().hashCode() : 0;
        result = 31 * result + (getStudyName() != null ? getStudyName().hashCode() : 0);
        result = 31 * result + (getLastSearchDate() != null ? getLastSearchDate().hashCode() : 0);
        result = 31 * result + (getResearchQuestions() != null ? getResearchQuestions().hashCode() : 0);
        result = 31 * result + (getQueries() != null ? getQueries().hashCode() : 0);
        result = 31 * result + (getLibraries() != null ? getLibraries().hashCode() : 0);
        return result;
    }
}

