package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.google.common.base.CharMatcher;

/**
 * Checks if the BibEntry contains unescaped ampersands.
 */
public class AmpersandChecker implements EntryChecker {
    // matches for an & preceded by any number of \
    private static final Pattern BACKSLASH_PRECEDED_AMPERSAND = Pattern.compile("\\\\*&");

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();

        for (Map.Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            // counts the number of even \ occurrences preceding an &
            long unescapedAmpersands = BACKSLASH_PRECEDED_AMPERSAND.matcher(field.getValue())
                    .results()
                    .map(MatchResult::group)
                    .filter(m -> CharMatcher.is('\\').countIn(m) % 2 == 0)
                    .count();

            if (unescapedAmpersands > 0) {
                results.add(new IntegrityMessage(Localization.lang("Found %0 unescaped '&'", unescapedAmpersands), entry, field.getKey()));
                // note: when changing the message - also do so in tests
            }
        }
        return results;
    }
}
