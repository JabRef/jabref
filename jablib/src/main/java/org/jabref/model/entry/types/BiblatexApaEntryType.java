package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BiblatexApaEntryType implements EntryType {

    Legislation("Legislation"),
    Legadminmaterial("Legadminmaterial"),
    Jurisdiction("Jurisdiction"),
    Constitution("Constitution"),
    Legal("Legal");

    private final String displayName;

    BiblatexApaEntryType(String displayName) {
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

    public static Optional<BiblatexApaEntryType> fromName(String name) {
        return Arrays.stream(BiblatexApaEntryType.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
    }
}
