package org.jabref.model.search.rules;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.PdfSearchResults;

public interface SearchRule {

    boolean applyRule(String query, BibEntry bibEntry);

    PdfSearchResults getFulltextResults(String query, BibEntry bibEntry);

    boolean validateSearchStrings(String query);
}
