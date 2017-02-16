package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.entry.MonthUtil;

public class NormalizeMonthFormatter implements Formatter {

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
        MonthUtil.Month month = MonthUtil.getMonth(value);
        if (month.isValid()) {
            return month.bibtexFormat;
        } else {
            return value;
        }
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
