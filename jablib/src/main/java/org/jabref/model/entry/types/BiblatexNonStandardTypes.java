package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum BiblatexNonStandardTypes implements EntryType {
    Artwork("artwork"),
    Audio("audio"),
    Bibnote("bibnote"),
    Commentary("commentary"),
    Image("image"),
    Jurisdiction("jurisdiction"),
    Legislation("legislation"),
    Legal("legal"),
    Letter("letter"),
    Movie("movie"),
    Music("music"),
    Performance("performance"),
    Review("review"),
    Standard("standard"),
    Video("video");

    private final String displayName;

    BiblatexNonStandardTypes(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<BiblatexNonStandardTypes> fromName(String name) {
        return Arrays.stream(BiblatexNonStandardTypes.values())
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
