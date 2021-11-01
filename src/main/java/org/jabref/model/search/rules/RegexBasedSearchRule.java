package org.jabref.model.search.rules;

import java.util.EnumSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search rule for regex-based search.
 */
@AllowedToUseLogic("Because access to the lucene index is needed")
public class RegexBasedSearchRule extends FullTextSearchRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegexBasedSearchRule.class);

    public RegexBasedSearchRule(EnumSet<SearchFlags> searchFlags) {
        super(searchFlags);
    }

    @Override
    public boolean validateSearchStrings(String query) {
        try {
            Pattern.compile(query, searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(query, searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            LOGGER.debug("Could not compile regex {}", query, ex);
            return false;
        }

        for (Field field : bibEntry.getFields()) {
            Optional<String> fieldOptional = bibEntry.getField(field);
            if (fieldOptional.isPresent()) {
                String fieldContentNoBrackets = bibEntry.getLatexFreeField(field).get();
                Matcher m = pattern.matcher(fieldContentNoBrackets);
                if (m.find()) {
                    return true;
                }
            }
        }
        return getFulltextResults(query, bibEntry).numSearchResults() > 0;
    }

}
