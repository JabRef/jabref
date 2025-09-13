package org.jabref.model.study;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StudyMetadata {
    @JsonProperty("created-date")
    private String createdDate;

    @JsonProperty("last-modified")
    private String lastModified;

    @JsonProperty("study-type")
    private String studyType = "systematic-literature-review";

    private String notes; // optional

    // Default constructor - no required fields
    public StudyMetadata() {
    }

    // Convenience constructors for common use cases
    public StudyMetadata(String createdDate) {
        this.createdDate = createdDate;
    }

    public StudyMetadata(String createdDate, String lastModified) {
        this.createdDate = createdDate;
        this.lastModified = lastModified;
    }

    public StudyMetadata(String createdDate, String lastModified, String studyType) {
        this.createdDate = createdDate;
        this.lastModified = lastModified;
        this.studyType = studyType;
    }

    public StudyMetadata(String createdDate, String lastModified, String studyType, String notes) {
        this.createdDate = createdDate;
        this.lastModified = lastModified;
        this.studyType = studyType;
        this.notes = notes;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getStudyType() {
        return studyType;
    }

    public void setStudyType(String studyType) {
        this.studyType = studyType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        StudyMetadata that = (StudyMetadata) other;
        return Objects.equals(createdDate, that.createdDate) &&
               Objects.equals(lastModified, that.lastModified) &&
               Objects.equals(studyType, that.studyType) &&
               Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdDate, lastModified, studyType, notes);
    }

    @Override
    public String toString() {
        return "StudyMetadata{" +
                "createdDate='" + createdDate + '\'' +
                ", lastModified='" + lastModified + '\'' +
                ", studyType='" + studyType + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
