package org.jabref.logic.integrity;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks if the BibEntry contains unescaped ampersands.
 */
public class AmpersandChecker implements EntryChecker{
    private static final Pattern UNESCAPED_AMPERSAND = Pattern.compile("(?<!\\\\)&");

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();

        for (Map.Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            Matcher matcher = UNESCAPED_AMPERSAND.matcher(field.getValue());
            if (matcher.find()) {
                results.add(new IntegrityMessage(Localization.lang("Found unescaped '&'"), entry, field.getKey()));
            }
        }
        return results;
    }
}
