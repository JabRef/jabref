package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

/**
 * Checks, if there are any HTML encoded characters in nonverbatim fields.
 */
public class HTMLCharacterChecker implements EntryChecker {
    // Detect any HTML encoded character
    private static final Pattern HTML_CHARACTER_PATTERN = Pattern.compile("&[#\\p{Alnum}]+;");

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();
        for (Map.Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            // skip verbatim fields
            if (field.getKey().getProperties().contains(FieldProperty.VERBATIM)) {
                continue;
            }

            Matcher characterMatcher = HTML_CHARACTER_PATTERN.matcher(field.getValue());
            if (characterMatcher.find()) {
                results.add(
                        new IntegrityMessage(Localization.lang("HTML encoded character found"), entry, field.getKey()));
            }
        }
        return results;
    }
}
