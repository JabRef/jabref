package org.jabref.logic.ai.util;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public final class CitationKeyCheck {

    private CitationKeyCheck() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean citationKeyIsPresentAndUnique(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        return !hasEmptyCitationKey(bibEntry) && bibEntry.getCitationKey().map(key -> citationKeyIsUnique(bibDatabaseContext, key)).orElse(false);
    }

    private static boolean hasEmptyCitationKey(BibEntry bibEntry) {
        return bibEntry.getCitationKey().map(String::isEmpty).orElse(true);
    }

    private static boolean citationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) == 1;
    }
}
