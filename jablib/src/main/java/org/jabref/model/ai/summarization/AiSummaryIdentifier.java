package org.jabref.model.ai.summarization;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public record AiSummaryIdentifier(String libraryId, String summaryName) {
    public static Optional<AiSummaryIdentifier> from(BibDatabaseContext ctx, BibEntry entry) {
        if (ctx.getMetaData().getAiLibraryId().isEmpty() || entry.getCitationKey().isEmpty()) {
            return Optional.empty();
        }

        String citationKey = entry.getCitationKey().get();
        if (ctx.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) != 1) {
            return Optional.empty();
        }

        return Optional.of(fromChecked(ctx, entry));
    }

    public static AiSummaryIdentifier fromChecked(BibDatabaseContext ctx, BibEntry entry) {
        assert ctx.getMetaData().getAiLibraryId().isPresent();
        assert entry.getCitationKey().isPresent();

        return new AiSummaryIdentifier(
                ctx.getMetaData().getAiLibraryId().get(),
                entry.getCitationKey().get()
        );
    }
}
