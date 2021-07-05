package org.jabref.model.search.rules;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.PdfSearchResults;

public interface SearchRule {

    boolean applyRule(String query, BibDatabaseContext databaseContext, BibEntry bibEntry);

    PdfSearchResults getFulltextResults(String query, BibDatabaseContext databaseContext, BibEntry bibEntry);

    boolean validateSearchStrings(String query);
}
