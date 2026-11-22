package org.jabref.logic.integrity;

import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

/// Checks if the BibEntry contains unescaped percent signs (`%`).
/// This is done in non-verbatim LaTeX fields, similar to [AmpersandChecker].
///
/// The [LatexIntegrityChecker] cannot detect unescaped `%` because SnuggleTeX treats `%` as a LaTeX comment.
public class PercentChecker implements EntryChecker {
    // Matches a '%' preceded by any number of '\'
    private static final Pattern BACKSLASH_PRECEDED_PERCENT = Pattern.compile("\\\\*%");

    private record FieldCount(Field field, long count) {
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldMap().entrySet().stream()
                    .filter(field -> FieldFactory.isLatexField(field.getKey()))
                    .flatMap(PercentChecker::getUnescapedPercentsWithCount)
                    .map(result -> new IntegrityMessage(
                            Localization.lang("Found %0 unescaped '%'", result.count()),
                            entry,
                            result.field()))
                    .toList();
    }

    private static Stream<FieldCount> getUnescapedPercentsWithCount(Map.Entry<Field, String> entry) {
        // An unescaped '%' has an even number of preceding '\', meaning the full match length is odd.
        long unescapedPercents = BACKSLASH_PRECEDED_PERCENT.matcher(entry.getValue())
                                                           .results()
                                                           .map(MatchResult::group)
                                                           .filter(match -> match.length() % 2 == 1)
                                                           .count();
        if (unescapedPercents == 0) {
            return Stream.empty();
        }
        return Stream.of(new FieldCount(entry.getKey(), unescapedPercents));
    }
}
