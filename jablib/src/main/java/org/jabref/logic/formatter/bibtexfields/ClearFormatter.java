package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

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
    public String format(@NonNull String oldString) {
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
