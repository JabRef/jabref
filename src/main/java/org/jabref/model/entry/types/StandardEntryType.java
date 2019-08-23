package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

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
    WWW("WWW");

    private final String displayName;

    StandardEntryType(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<StandardEntryType> fromName(String name) {
        return Arrays.stream(StandardEntryType.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
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
