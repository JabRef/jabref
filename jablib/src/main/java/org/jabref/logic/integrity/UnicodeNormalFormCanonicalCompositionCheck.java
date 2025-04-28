package org.jabref.logic.integrity;

import java.text.Normalizer;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * Detect any Unicode characters that is not in NFC format. NFC:  <a href="https://en.wikipedia.org/wiki/Unicode_equivalence#Normal_forms">Normal form "Normalization Form Canonical Composition" (NFC)</a>: Characters are decomposed and then recomposed by canonical equivalence.
 *
 * Normalizer: {@link org.jabref.logic.formatter.bibtexfields.NormalizeUnicodeFormatter}
 */
public class UnicodeNormalFormCanonicalCompositionCheck implements EntryChecker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldMap()
                    .entrySet()
                    .stream()
                    .filter(field -> !Normalizer.isNormalized(field.getValue(), Normalizer.Form.NFC))
                    .map(field -> new IntegrityMessage(Localization.lang("Value is not in Unicode's Normalization Form \"Canonical Composition\" (NFC) format"), entry,
                            field.getKey()))
                    .toList();
    }
}
