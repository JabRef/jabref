package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.MonthUtil;

import java.util.Objects;

public class MonthFormatter implements Formatter {

    @Override
    public String getName() {
        return "Month";
    }

    @Override
    public String getKey() {
        return "MonthFormatter";
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
        return Localization.lang("Normalizes content of %s to the format #mon#.");
    }
}
