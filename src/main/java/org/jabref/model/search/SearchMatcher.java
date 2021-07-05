package org.jabref.model.search;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface SearchMatcher {
    boolean isMatch(BibDatabaseContext databaseContext, BibEntry entry);
}
