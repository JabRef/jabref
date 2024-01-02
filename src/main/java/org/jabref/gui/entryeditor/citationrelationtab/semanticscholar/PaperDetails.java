package org.jabref.gui.entryeditor.citationrelationtab.semanticscholar;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class PaperDetails {
    private String paperId;
    private String title;
    private String year;
    private int citationCount;
    private int referenceCount;
    private List<AuthorResponse> authors;

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getCitationCount() {
        return citationCount;
    }

    public void setCitationCount(int citationCount) {
        this.citationCount = citationCount;
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(int referenceCount) {
        this.referenceCount = referenceCount;
    }

    public List<AuthorResponse> getAuthors() {
        return authors;
    }

    public BibEntry toBibEntry() {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.TITLE, getTitle());
        if (getYear() != null) {
            bibEntry.setField(StandardField.YEAR, getYear());
        }

        String authors = getAuthors().stream()
                                     .map(AuthorResponse::getName)
                                     .collect(Collectors.joining(" and "));
        bibEntry.setField(StandardField.AUTHOR, authors);

        return bibEntry;
    }

    @Override
    public String toString() {
        return "PaperDetails{" +
                "paperId='" + paperId + '\'' +
                ", title='" + title + '\'' +
                ", year='" + year + '\'' +
                ", citationCount=" + citationCount +
                ", referenceCount=" + referenceCount +
                '}';
    }
}
