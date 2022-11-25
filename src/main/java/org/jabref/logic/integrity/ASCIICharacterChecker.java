package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.google.common.base.CharMatcher;

public class ASCIICharacterChecker implements EntryChecker {

    /**
     * Detect any non ASCII encoded characters, e.g., umlauts or unicode in the fields
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();
        for (Map.Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            boolean asciiOnly = CharMatcher.ascii().matchesAllOf(field.getValue());
            if (!asciiOnly) {
                results.add(new IntegrityMessage(Localization.lang("Non-ASCII encoded character found"), entry,
                        field.getKey()));
            }
        }
        return results;
    }
}
