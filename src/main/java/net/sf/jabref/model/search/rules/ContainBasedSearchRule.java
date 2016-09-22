package net.sf.jabref.model.search.rules;

import java.util.Iterator;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.strings.LatexToUnicode;

/**
 * Search rule for contain-based search.
 */
public class ContainBasedSearchRule implements SearchRule {

    private static final LatexToUnicode LATEX_TO_UNICODE_FORMATTER = new LatexToUnicode();

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
            searchString = searchString.toLowerCase();
        }

        List<String> unmatchedWords = new SentenceAnalyzer(searchString).getWords();

        for (String fieldContent : bibEntry.getFieldValues()) {
            String formattedFieldContent = LATEX_TO_UNICODE_FORMATTER.format(fieldContent);
            if (!caseSensitive) {
                formattedFieldContent = formattedFieldContent.toLowerCase();
            }

            Iterator<String> unmatchedWordsIterator = unmatchedWords.iterator();
            while (unmatchedWordsIterator.hasNext()) {
                String word = unmatchedWordsIterator.next();
                if(formattedFieldContent.contains(word)) {
                    unmatchedWordsIterator.remove();
                }
            }

            if(unmatchedWords.isEmpty()) {
                return true;
            }
        }

        return false; // Didn't match all words.
    }

}
