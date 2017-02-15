package net.sf.jabref.model.search;

import net.sf.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface SearchMatcher {

    boolean isMatch(BibEntry entry);

}
