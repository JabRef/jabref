package org.jabref.gui.entryeditor.semanticscholar;

public class PaperDetails {
    private String paperId;
    private String title;
    private String year;
    private int citationCount;
    private int referenceCount;

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
