package org.jabref.logic.formatter.bibtexfields;

import java.text.Normalizer;
import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;

/**
 * Clean up field values by formatting Unicode values with Normalize Unicode
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
        Objects.requireNonNull(value);

        String normalizedValue = Normalizer.normalize(value, Normalizer.Form.NFC);

        return normalizedValue;
    }
}
