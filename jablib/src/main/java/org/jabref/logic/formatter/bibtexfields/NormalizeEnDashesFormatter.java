package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

public class NormalizeEnDashesFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Normalize en dashes");
    }

    @Override
    public String getKey() {
        return "normalize_en_dashes";
    }

    @Override
    public String format(@NonNull String value) {
        return value.replaceAll(" - ", " -- ");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalizes the en dashes.");
    }

    @Override
    public String getExampleInput() {
        return "Winery - A Modeling Tool for TOSCA-based Cloud Applications";
    }
}
