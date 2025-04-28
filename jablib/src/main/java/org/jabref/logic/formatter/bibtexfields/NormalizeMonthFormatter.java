package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.Month;

public class NormalizeMonthFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Normalize month");
    }

    @Override
    public String getKey() {
        return "normalize_month";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);
        Optional<Month> month = Month.parse(value);
        return month.map(Month::getJabRefFormat).orElse(value);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalize month to BibTeX standard abbreviation.");
    }

    @Override
    public String getExampleInput() {
        return "December";
    }
}
