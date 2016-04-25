package net.sf.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.MonthUtil;

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
