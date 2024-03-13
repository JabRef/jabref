package org.jabref.gui.entryeditor.citationrelationtab.semanticscholar;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.gson.annotations.SerializedName;

public class PaperDetails {
    private String paperId;
    private String title;
    private String year;

    @SerializedName("abstract")
    private String abstr;
    private String url;
    private int citationCount;
    private int referenceCount;
    private List<AuthorResponse> authors;
    private List<String> publicationTypes;
    private Map<String, String> externalIds;

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

    public String getAbstract() {
        return abstr;
    }

    public void setAbstract(String abstr) {
        this.abstr = abstr;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
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

    public String getPublicationType() {
        if (publicationTypes == null || publicationTypes.isEmpty()) {
            return "Misc";
        }
        if (publicationTypes.contains("Conference")) {
            return "InProceedings";
        } else if (publicationTypes.contains("JournalArticle")) {
            return "Article";
        } else {
            return switch (publicationTypes.getFirst()) {
                case "Review" ->
                        "Misc";
                case "CaseReport" ->
                        "Report";
                case "ClinicalTrial" ->
                        "Report";
                case "Dataset" ->
                        "Dataset";
                case "Editorial" ->
                        "Misc";
                case "LettersAndComments" ->
                        "Misc";
                case "MetaAnalysis" ->
                        "Article";
                case "News" ->
                        "Misc";
                case "Study" ->
                        "Article";
                case "Book" ->
                        "Book";
                case "BookSection" ->
                        "InBook";
                default ->
                        "Misc";
            };
        }
    }

    public String getDOI() {
        if (externalIds != null) {
            if (externalIds.containsKey("DOI")) {
                return externalIds.get("DOI");
            } else if (externalIds.containsKey("ArXiv")) {
                // Some ArXiv articles don't return the DOI, even though it's easy to obtain from the ArXiv ID
                return "10.48550/arXiv." + externalIds.get("ArXiv");
            }
        }
        return "";
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

        bibEntry.setType(StandardEntryType.valueOf(getPublicationType()));

        if (getDOI() != null) {
            bibEntry.setField(StandardField.DOI, getDOI());
        }

        if (getURL() != null) {
            bibEntry.setField(StandardField.URL, getURL());
        }

        if (getAbstract() != null) {
            bibEntry.setField(StandardField.ABSTRACT, getAbstract());
        }

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
