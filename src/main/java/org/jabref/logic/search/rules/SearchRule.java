package org.jabref.logic.search.rules;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.PdfSearchResults;

public interface SearchRule {

    /**
     * This method applies the rule to the given BibEntry.
     *
     * @return true iff there is a match
     */
    boolean applyRule(String query, BibEntry bibEntry);

    /**
     * Executes a search on the linked PDF file(s) of the given BibEntry.
     */
    PdfSearchResults getFulltextResults(String query, BibEntry bibEntry);

    boolean validateSearchStrings(String query);
}
