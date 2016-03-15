package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import java.util.Objects;

public class TrimFormatter implements Formatter {

    @Override
    public String getName() {
        return "Trim whitespace";
    }

    @Override
    public String getKey() {
        return "TrimFormatter";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);
        return value.trim();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes any leading and trailing whitespace in %s.");
    }
}
