package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.regex.Matcher;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class EscapeDollarSignFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Escape dollar sign");
    }

    @Override
    public String getKey() {
        return "escapeDollarSign";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);
        return value.replaceAll("(?<!\\\\)\\$", Matcher.quoteReplacement("\\$"));
    }

    @Override
    public String getDescription() {
        return Localization.lang("Escape dollar sign");
    }

    @Override
    public String getExampleInput() {
        return "Text$with$dollar$sign";
    }
}
