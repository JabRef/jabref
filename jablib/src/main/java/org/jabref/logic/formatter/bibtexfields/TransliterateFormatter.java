package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.Transliteration;

public class TransliterateFormatter extends Formatter {
    @Override
    public String getName() {
        return Localization.lang("Transliterate");
    }

    @Override
    public String getKey() {
        return "transliterate";
    }

    @Override
    public String format(String value) {
        return Transliteration.transliterate(value);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts non-Latin characters to their Latin equivalents.");
    }

    @Override
    public String getExampleInput() {
        return "Карпенко Надежда";
    }
}
