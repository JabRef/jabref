package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.regex.Matcher;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class EscapeCurrencySymbolsFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Escape currency symbols");
    }

    @Override
    public String getKey() {
        return "escapeCurrencySymbols";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);
        String replacement = "\\$";
        return value.replaceAll("(?<!\\\\)\\$", Matcher.quoteReplacement(replacement));
    }

    @Override
    public String getDescription() {
        return Localization.lang("Escape currency symbols");
    }

    @Override
    public String getExampleInput() {
        return "Text$with$currency$symbols";
    }
}
