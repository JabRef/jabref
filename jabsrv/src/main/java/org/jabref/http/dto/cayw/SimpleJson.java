package org.jabref.http.dto.cayw;

import java.nio.charset.StandardCharsets;

import org.jabref.model.entry.BibEntry;

import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SimpleJson(
        long id,
        String citationKey) {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJson.class);

    public static SimpleJson fromBibEntry(BibEntry bibEntry) {
        if (bibEntry.getCitationKey().isEmpty()) {
            LOGGER.warn("BibEntry has no citation key: {}", bibEntry);
            return new SimpleJson(0, "");
        }

        long id = Hashing.sha256().hashString(bibEntry.getCitationKey().get(), StandardCharsets.UTF_8).asLong();
        String citationKey = bibEntry.getCitationKey().get();
        return new SimpleJson(id, citationKey);
    }
}
