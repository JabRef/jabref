package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SystematicLiteratureReviewStudyEntryType implements EntryType {
    STUDY_ENTRY("Study"),
    SEARCH_QUERY_ENTRY("SearchQuery"),
    LIBRARY_ENTRY("Library");

    private final String displayName;

    SystematicLiteratureReviewStudyEntryType(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<SystematicLiteratureReviewStudyEntryType> fromName(String name) {
        return Arrays.stream(SystematicLiteratureReviewStudyEntryType.values())
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
