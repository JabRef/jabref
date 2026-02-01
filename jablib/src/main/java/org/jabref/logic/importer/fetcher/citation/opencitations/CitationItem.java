package org.jabref.logic.importer.fetcher.citation.opencitations;

import java.util.ArrayList;
import java.util.List;

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

    List<IdentifierWithField> extractIdentifiers(@Nullable String pidString) {
        if (pidString == null || pidString.isEmpty()) {
            return List.of();
        }

        String[] pids = pidString.split("\\s+");
        List<IdentifierWithField> identifiers = new ArrayList<>();
        for (String pid : pids) {
            int colonIndex = pid.indexOf(':');
            if (colonIndex > 0) {
                String prefix = pid.substring(0, colonIndex);
                String value = pid.substring(colonIndex + 1);
                Field field = FieldFactory.parseField(prefix);
                identifiers.add(new IdentifierWithField(field, value));
            }
        }

        return identifiers;
    }

    List<IdentifierWithField> citingIdentifiers() {
        return extractIdentifiers(citing);
    }

    List<IdentifierWithField> citedIdentifiers() {
        return extractIdentifiers(cited);
    }
}
