package org.jabref.http.dto.cayw;

import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SimpleJson(
        long id,
        String citationKey,
        @Nullable String locator,
        @Nullable String prefix,
        @Nullable String suffix,
        boolean suppressAuthor) {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJson.class);

    public static SimpleJson fromBibEntry(BibEntry bibEntry, CitationProperties properties) {
        if (bibEntry.getCitationKey().isEmpty()) {
            LOGGER.warn("BibEntry has no citation key: {}", bibEntry);
            return new SimpleJson(-1, "", null, null, null, false);
        }

        return new SimpleJson(
                bibEntry.getSharedBibEntryData().getSharedID(),
                bibEntry.getCitationKey().get(),
                properties.getFormattedLocator().orElse(null),
                properties.getPrefix().orElse(null),
                properties.getSuffix().orElse(null),
                properties.isOmitAuthor()
        );
    }
}
