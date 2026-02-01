package org.jabref.logic.importer.fetcher.citation.opencitations;

import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class CitationItem {
    @Nullable String oci;
    @Nullable String citing;
    @Nullable String cited;
    @Nullable String creation;
    @Nullable String timespan;

    @SerializedName("journal_sc")
    @Nullable String journalSelfCitation;

    @SerializedName("author_sc")
    @Nullable String authorSelfCitation;

    record IdentifierWithField(Field field, String value) {}

    Optional<IdentifierWithField> extractBestIdentifier(@Nullable String pidString) {
        if (pidString == null || pidString.isEmpty()) {
            return Optional.empty();
        }

        String[] pids = pidString.split("\\s+");
        for (String pid : pids) {
            int colonIndex = pid.indexOf(':');
            if (colonIndex > 0) {
                String prefix = pid.substring(0, colonIndex);
                String value = pid.substring(colonIndex + 1);
                Field field = FieldFactory.parseField(prefix);
                return Optional.of(new IdentifierWithField(field, value));
            }
        }

        return Optional.empty();
    }

    Optional<IdentifierWithField> citingIdentifier() {
        return extractBestIdentifier(citing);
    }

    Optional<IdentifierWithField> citedIdentifier() {
        return extractBestIdentifier(cited);
    }
}
