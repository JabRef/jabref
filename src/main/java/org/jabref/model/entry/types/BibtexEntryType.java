package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BibtexEntryType implements EntryType {
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
    Unpublished("Unpublished");

    private final String displayName;

    BibtexEntryType(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<BibtexEntryType> fromName(String name) {
        return Arrays.stream(BibtexEntryType.values())
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
