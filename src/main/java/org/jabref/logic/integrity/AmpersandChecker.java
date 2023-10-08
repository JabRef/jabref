package org.jabref.logic.integrity;

import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.util.Pair;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

import com.google.common.base.CharMatcher;

/**
 * Checks if the BibEntry contains unescaped ampersands.
 * This is done in nonverbatim fields. Similar to {@link HTMLCharacterChecker}
 *
 * The {@link LatexIntegrityChecker} is not able to check unescaped ampersands. Therefore, this separate checker is required.
 */
public class AmpersandChecker implements EntryChecker {
    // matches for an & preceded by any number of \
    private static final Pattern BACKSLASH_PRECEDED_AMPERSAND = Pattern.compile("\\\\*&");

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldMap().entrySet().stream()
                    .filter(field -> !field.getKey().getProperties().contains(FieldProperty.VERBATIM))
                    // We use "flatMap" instead of filtering later, because we assume there won't be that much error messages - and construction of Stream.empty() is faster than construction of a new Tuple2 (including lifting long to Long)
                    .flatMap(AmpersandChecker::getUnescapedAmpersandsWithCount)
                    .map(pair -> new IntegrityMessage(Localization.lang("Found %0 unescaped '&'", pair.getValue()), entry, pair.getKey()))
                    .toList();
    }

    private static Stream<Pair<Field, Long>> getUnescapedAmpersandsWithCount(Map.Entry<Field, String> entry) {
        // counts the number of even \ occurrences preceding an &
        long unescapedAmpersands = BACKSLASH_PRECEDED_AMPERSAND.matcher(entry.getValue())
                                                               .results()
                                                               .map(MatchResult::group)
                                                               .filter(m -> CharMatcher.is('\\').countIn(m) % 2 == 0)
                                                               .count();
        if (unescapedAmpersands == 0) {
            return Stream.empty();
        }
        return Stream.of(new Pair<>(entry.getKey(), unescapedAmpersands));
    }
}
