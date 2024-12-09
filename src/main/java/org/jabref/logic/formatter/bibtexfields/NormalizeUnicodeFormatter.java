package org.jabref.logic.formatter.bibtexfields;

import java.text.Normalizer;

import org.jabref.logic.cleanup.Formatter;

/**
 * Clean up field values by formatting Unicode values by using the <a href="https://en.wikipedia.org/wiki/Unicode_equivalence#Normal_forms">Normal form "Normalization Form Canonical Composition" (NFC)</a>: Characters are decomposed and then recomposed by canonical equivalence.
 *
 * The {@link org.jabref.logic.integrity.UnicodeNormalFormCanonicalCompositionCheck} is for checking the presence of other Unicode representations.
 */
public class NormalizeUnicodeFormatter extends Formatter {

    @Override
    public String getName() {
        return "Normalize Unicode";
    }

    @Override
    public String getKey() {
        return "NORMALIZE_UNICODE";
    }

    @Override
    public String getDescription() {
        return "Normalize Unicode characters in BibTeX fields.";
    }

    @Override
    public String getExampleInput() {
        return "H\u00E9ll\u00F4 W\u00F6rld";
    }

    @Override
    public String format(String value) {
        String normalizedValue = Normalizer.normalize(value, Normalizer.Form.NFC);
        return normalizedValue;
    }
}
