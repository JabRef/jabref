package org.jabref.logic.integrity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * Detect any Unicode characters that is not in NFC format
 */
public class UnicodeNormalFormCCheck implements EntryChecker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();
        for (Map.Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            String normalizedString = Normalizer.normalize(field.getValue(), Normalizer.Form.NFC);
            if (!(field.getValue().equals(normalizedString))) {
                results.add(new IntegrityMessage(Localization.lang("Value is not in Normal Form C (NFC) format"), entry,
                        field.getKey()));
            }
        }
        return results;
    }
}
