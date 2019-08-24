package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum IEEETranEntryType implements EntryType {
    IEEEtranBSTCTL("IEEEtranBSTCTL"),
    Electronic("Electronic"),
    Patent("Patent"),
    Periodical("Periodical"),
    Standard("Standard");

    private final String displayName;

    IEEETranEntryType(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<IEEETranEntryType> fromName(String name) {
        return Arrays.stream(IEEETranEntryType.values())
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
