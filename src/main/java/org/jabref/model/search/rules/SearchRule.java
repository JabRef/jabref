package org.jabref.model.search.rules;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.LuceneSearchResults;

public interface SearchRule {

    boolean applyRule(String query, BibEntry bibEntry);

    LuceneSearchResults getLuceneResults(String query, BibEntry bibEntry);

    boolean validateSearchStrings(String query);
}
