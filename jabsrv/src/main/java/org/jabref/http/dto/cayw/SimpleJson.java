package org.jabref.http.dto.cayw;

import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SimpleJson(
        long id,
        String citationKey) {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJson.class);

    public static SimpleJson fromBibEntry(BibEntry bibEntry) {
        if (bibEntry.getCitationKey().isEmpty()) {
            LOGGER.warn("BibEntry has no citation key: {}", bibEntry);
            return new SimpleJson(-1, "");
        }

        return new SimpleJson(bibEntry.getSharedBibEntryData().getSharedID(), bibEntry.getCitationKey().get());
    }
}
