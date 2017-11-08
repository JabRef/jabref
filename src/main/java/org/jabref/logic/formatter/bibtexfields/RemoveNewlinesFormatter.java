package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

/**
 * Removes all line breaks in the string.
 */
public class RemoveNewlinesFormatter implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Remove line breaks");
    }

    @Override
    public String getKey() {
        return "remove_newlines";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        return value.replace("\r\n", " ").replace("\n", " ").trim();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes all line breaks in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "In \n CDMA";
    }
}
