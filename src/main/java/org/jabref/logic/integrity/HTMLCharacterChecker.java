package org.jabref.logic.integrity;

import java.util.List;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldProperty;

/**
 * Checks, if there are any HTML encoded characters in nonverbatim fields.
 */
public class HTMLCharacterChecker implements EntryChecker {
    // Detect any HTML encoded character
    private static final Pattern HTML_CHARACTER_PATTERN = Pattern.compile("&[#\\p{Alnum}]+;");

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldMap().entrySet().stream()
                    .filter(field -> !field.getKey().getProperties().contains(FieldProperty.VERBATIM))
                    .filter(field -> HTML_CHARACTER_PATTERN.matcher(field.getValue()).find())
                    .map(field -> new IntegrityMessage(Localization.lang("HTML encoded character found"), entry, field.getKey()))
                    .toList();
    }
}
