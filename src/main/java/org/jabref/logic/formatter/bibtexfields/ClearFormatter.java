package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class ClearFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Clear");
    }

    @Override
    public String getKey() {
        return "clear";
    }

    @Override
    public String format(String oldString) {
        Objects.requireNonNull(oldString);
        return "";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Clears the field completely.");
    }

    @Override
    public String getExampleInput() {
        return "Obsolete text";
    }
}
