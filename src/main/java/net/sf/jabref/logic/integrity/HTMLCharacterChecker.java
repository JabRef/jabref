package net.sf.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class HTMLCharacterChecker implements Checker {
    // Detect any HTML encoded character,
    private static final Pattern HTML_CHARACTER_PATTERN = Pattern.compile("&[#\\p{Alnum}]+;");

    /**
     * Checks, if there are any HTML encoded characters in the fields.
     *
     * This check excludes the `url` field as it allows HTML encoded characters.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();
        for (Map.Entry<String, String> field : entry.getFieldMap().entrySet()) {
            // skip verbatim URL field
            if (field.getKey().equals(FieldName.URL)) {
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
