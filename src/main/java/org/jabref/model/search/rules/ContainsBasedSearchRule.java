package org.jabref.model.search.rules;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
import org.jabref.model.strings.StringUtil;

/**
 * Search rule for a search based on String.contains()
 */
@AllowedToUseLogic("Because access to the lucene index is needed")
public class ContainsBasedSearchRule extends FullTextSearchRule {

    public ContainsBasedSearchRule(EnumSet<SearchFlags> searchFlags) {
        super(searchFlags);
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        String searchString = query;
        if (!searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
            searchString = searchString.toLowerCase(Locale.ROOT);
        }

        List<String> unmatchedWords = new SentenceAnalyzer(searchString).getWords();

        for (Field fieldKey : bibEntry.getFields()) {
            String formattedFieldContent = StringUtil.stripAccents(bibEntry.getLatexFreeField(fieldKey).get());
            if (!searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
                formattedFieldContent = formattedFieldContent.toLowerCase(Locale.ROOT);
            }

            Iterator<String> unmatchedWordsIterator = unmatchedWords.iterator();
            while (unmatchedWordsIterator.hasNext()) {
                String word = StringUtil.stripAccents(unmatchedWordsIterator.next());
                if (formattedFieldContent.contains(word)) {
                    unmatchedWordsIterator.remove();
                }
            }

            if (unmatchedWords.isEmpty()) {
                return true;
            }
        }

        return getLuceneResults(query, bibEntry).numSearchResults() > 0; // Didn't match all words.
    }
}
