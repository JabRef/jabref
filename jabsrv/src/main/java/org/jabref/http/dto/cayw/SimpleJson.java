package org.jabref.http.dto.cayw;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;

import com.google.common.hash.Hashing;

public record SimpleJson(
        long id,
        String citationKey) {

    public SimpleJson {
        Objects.requireNonNull(citationKey, "CitationKey must not be null");
        if (citationKey.isBlank()) {
            throw new IllegalArgumentException("CitationKey must not be blank");
        }
    }

    public static SimpleJson fromBibEntry(BibEntry bibEntry) {
        if (bibEntry.getCitationKey().isEmpty()) {
            throw new IllegalArgumentException("BibEntry must have a citation key");
        }
        long id = Hashing.sha256().hashString(bibEntry.getCitationKey().get(), StandardCharsets.UTF_8).asLong();
        String citationKey = bibEntry.getCitationKey().get();
        return new SimpleJson(id, citationKey);
    }
}
