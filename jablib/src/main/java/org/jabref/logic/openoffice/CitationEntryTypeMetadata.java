package org.jabref.logic.openoffice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class CitationEntryTypeMetadata {

    static final int SCHEMA_VERSION = 1;

    int schemaVersion = SCHEMA_VERSION;
    @Nullable Map<String, @Nullable CitationEntryType> citationTypeMap = new LinkedHashMap<>();

    static class CitationEntryType {
        @Nullable String jabrefEntryType;

        CitationEntryType(String jabrefEntryType) {
            this.jabrefEntryType = jabrefEntryType;
        }
    }

    public static Map<String, @Nullable CitationEntryType> getCitationTypeMap(CitationEntryTypeMetadata metadata) {
        Map<String, @Nullable CitationEntryType> citationTypeMap = Optional.ofNullable(metadata.citationTypeMap)
                                                                           .orElseGet(LinkedHashMap::new);
        metadata.citationTypeMap = citationTypeMap;
        return citationTypeMap;
    }
}
