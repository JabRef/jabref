package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum StandardEntryType implements EntryType {
    Article("Article"),
    Book("Book"),
    BookInBook("BookInBook"),
    Booklet("Booklet"),
    Collection("Collection"),
    Conference("Conference"),
    Electronic("Electronic"),
    IEEEtranBSTCTL("IEEEtranBSTCTL"),
    InBook("InBook"),
    InCollection("InCollection"),
    InProceedings("InProceedings"),
    InReference("InReference"),
    Manual("Manual"),
    MastersThesis("MastersThesis"),
    Misc("Misc"),
    MvBook("MvBook"),
    MvCollection("MvCollection"),
    MvProceedings("MvProceedings"),
    MvReference("MvReference"),
    Online("Online"),
    Patent("Patent"),
    Periodical("Periodical"),
    PhdThesis("PhdThesis"),
    Proceedings("Proceedings"),
    Reference("Reference"),
    Report("Report"),
    Set("Set"),
    SuppBook("SuppBook"),
    SuppCollection("SuppCollection"),
    SuppPeriodical("SuppPeriodical"),
    TechReport("TechReport"),
    Thesis("Thesis"),
    Unpublished("Unpublished"),
    WWW("WWW"),
    Standard("Standard");

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
