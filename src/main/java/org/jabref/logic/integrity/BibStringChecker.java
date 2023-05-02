package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

/**
 * Checks, if there is an even number of unescaped # (FieldWriter.BIBTEX_STRING_START_END_SYMBOL)
 */
public class BibStringChecker implements EntryChecker {

    // Detect FieldWriter.BIBTEX_STRING_START_END_SYMBOL (#) if it doesn't have a \ in front of it or if it starts the string
    private static final Pattern UNESCAPED_HASH = Pattern.compile("(?<!\\\\)" + FieldWriter.BIBTEX_STRING_START_END_SYMBOL + "|^" + FieldWriter.BIBTEX_STRING_START_END_SYMBOL);

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();

        Map<Field, String> fields = entry.getFieldMap();

        for (Map.Entry<Field, String> field : fields.entrySet()) {
            if (!field.getKey().getProperties().contains(FieldProperty.VERBATIM)) {
                Matcher hashMatcher = UNESCAPED_HASH.matcher(field.getValue());
                int hashCount = 0;
                while (hashMatcher.find()) {
                    hashCount++;
                }
                if ((hashCount & 1) == 1) { // Check if odd
                    // # is FieldWriter.BIBTEX_STRING_START_END_SYMBOL
                    results.add(new IntegrityMessage(Localization.lang("odd number of unescaped '#'"), entry,
                            field.getKey()));
                }
            }
        }
        return results;
    }
}
