package org.jabref.logic.importer.fetcher.citation.opencitations;

import java.util.Optional;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

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

    Optional<String> extractDoi(@Nullable String pidString) {
        if (pidString == null || pidString.isEmpty()) {
            return Optional.empty();
        }

        String[] pids = pidString.split("\\s+");
        for (String pid : pids) {
            if (pid.startsWith("doi:")) {
                return Optional.of(pid.substring(4));
            }
        }
        return Optional.empty();
    }

    Optional<String> citingDoi() {
        return extractDoi(citing);
    }

    Optional<String> citedDoi() {
        return extractDoi(cited);
    }
}
