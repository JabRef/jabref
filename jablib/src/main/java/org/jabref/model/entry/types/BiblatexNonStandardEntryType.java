package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BiblatexNonStandardEntryType implements EntryType {

    Artwork("Artwork"),
    Audio("Audio"),
    Bibnote("Bibnote"),
    Commentary("Commentary"),
    Image("Image"),
    Jurisdiction("Jurisdiction"),
    Legislation("Legislation"),
    Legal("Legal"),
    Letter("Letter"),
    Movie("Movie"),
    Music("Music"),
    Performance("Performance"),
    Review("Review"),
    Standard("Standard"),
    Video("Video");

    private final String displayName;

    BiblatexNonStandardEntryType(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<BiblatexNonStandardEntryType> fromName(String name) {
        return Arrays.stream(BiblatexNonStandardEntryType.values())
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

