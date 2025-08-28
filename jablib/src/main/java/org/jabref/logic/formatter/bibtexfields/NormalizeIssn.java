package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.ISSN;

import org.jspecify.annotations.NonNull;

public class NormalizeIssn extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Normalize ISSN");
    }

    @Override
    public String getKey() {
        return "normalize_issn";
    }

    @Override
    public String format(@NonNull String value) {
        if (value.isBlank()) {
            return value;
        }

        ISSN issn = new ISSN(value);

        if (issn.isCanBeCleaned()) {
            return issn.getCleanedISSN();
        }
        return value;
    }

    @Override
    public String getDescription() {
        return "Normalizes ISSNs by ensuring they contain a dash (e.g., 12345678 → 1234-5678)";
    }

    @Override
    public String getExampleInput() {
        return "12345678";
    }
}
