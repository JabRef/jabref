package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

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
    public String format(String value) {
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
