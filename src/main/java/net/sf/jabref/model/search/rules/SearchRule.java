package net.sf.jabref.model.search.rules;

import net.sf.jabref.model.entry.BibEntry;

public interface SearchRule {

    boolean applyRule(String query, BibEntry bibEntry);

    boolean validateSearchStrings(String query);
}
