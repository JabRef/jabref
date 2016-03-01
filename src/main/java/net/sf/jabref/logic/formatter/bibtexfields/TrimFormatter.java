package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;

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
}
