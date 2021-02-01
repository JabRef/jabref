package org.jabref.model.entry.types;

import java.util.Locale;

public enum StandardEntryType implements EntryType {
    // BibTeX
    Article("Article"),
    Book("Book"),
    Booklet("Booklet"),
    Collection("Collection"),
    Conference("Conference"),
    InBook("InBook"),
    InCollection("InCollection"),
    InProceedings("InProceedings"),
    Manual("Manual"),
    MastersThesis("MastersThesis"),
    Misc("Misc"),
    PhdThesis("PhdThesis"),
    Proceedings("Proceedings"),
    TechReport("TechReport"),
    Unpublished("Unpublished"),
    // Biblatex
    BookInBook("BookInBook"),
    InReference("InReference"),
    MvBook("MvBook"),
    MvCollection("MvCollection"),
    MvProceedings("MvProceedings"),
    MvReference("MvReference"),
    Online("Online"),
    Reference("Reference"),
    Report("Report"),
    Set("Set"),
    SuppBook("SuppBook"),
    SuppCollection("SuppCollection"),
    SuppPeriodical("SuppPeriodical"),
    Thesis("Thesis"),
    WWW("WWW"),
    Software("Software"),
    Dataset("Dataset"),
    SoftwareVersion("SoftwareVersion"),
    SoftwareModule("SoftwareModule"),
    CodeFragment("CodeFragment");

    private final String displayName;

    StandardEntryType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return displayName.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
