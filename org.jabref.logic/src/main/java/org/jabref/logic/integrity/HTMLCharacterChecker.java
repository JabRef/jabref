package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.integrity.IntegrityCheck.Checker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

public class HTMLCharacterChecker implements Checker {
    // Detect any HTML encoded character,
    private static final Pattern HTML_CHARACTER_PATTERN = Pattern.compile("&[#\\p{Alnum}]+;");

    /**
     * Checks, if there are any HTML encoded characters in nonverbatim fields.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();
        for (Map.Entry<String, String> field : entry.getFieldMap().entrySet()) {
            // skip verbatim fields
            if (InternalBibtexFields.getFieldProperties(field.getKey()).contains(FieldProperty.VERBATIM)) {
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
