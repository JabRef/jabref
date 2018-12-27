package org.jabref.model.search.rules;

import org.jabref.model.entry.BibEntry;

public interface SearchRule {

    boolean applyRule(String query, BibEntry bibEntry);

    boolean validateSearchStrings(String query);
}
