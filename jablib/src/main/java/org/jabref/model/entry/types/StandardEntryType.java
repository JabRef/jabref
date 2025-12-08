package org.jabref.model.entry.types;

import java.util.Locale;

/**
 * Defines standard entry types as defined by BibTeX and BibLaTeX.
 * At {@link BibtexEntryTypeDefinitions}, the required and optional fields for each type (for BibTeX) is defined.
 * The BibLaTeX entry types are defined at {@link BiblatexEntryTypeDefinitions}.
 * More reading on BibTeX and its fields is collected at <a href="https://docs.jabref.org/advanced/fields">JabRef's documentation</a>.
 */
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
    // BibLaTeX
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
    Dataset("Dataset");

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
