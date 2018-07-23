package org.jabref.model.search.rules;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jabref.model.entry.BibEntry;

/**
 * Search rule for contain-based search.
 */
public class ContainBasedSearchRule implements SearchRule {

    private final boolean caseSensitive;

    public ContainBasedSearchRule(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {

        String searchString = query;
        if (!caseSensitive) {
            searchString = searchString.toLowerCase(Locale.ROOT);
        }

        List<String> unmatchedWords = new SentenceAnalyzer(searchString).getWords();

        for (String fieldKey : bibEntry.getFieldNames()) {
            String formattedFieldContent = bibEntry.getLatexFreeField(fieldKey).get();
            if (!caseSensitive) {
                formattedFieldContent = formattedFieldContent.toLowerCase(Locale.ROOT);
            }

            Iterator<String> unmatchedWordsIterator = unmatchedWords.iterator();
            while (unmatchedWordsIterator.hasNext()) {
                String word = unmatchedWordsIterator.next();
                if (formattedFieldContent.contains(word)) {
                    unmatchedWordsIterator.remove();
                }
            }

            if (unmatchedWords.isEmpty()) {
                return true;
            }
        }

        return false; // Didn't match all words.
    }

}
