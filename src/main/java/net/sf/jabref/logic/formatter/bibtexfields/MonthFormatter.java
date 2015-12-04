package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.model.entry.MonthUtil;

public class MonthFormatter implements Formatter {

    @Override
    public String getName() {
        return "Month";
    }

    @Override
    public String format(String value) {
        MonthUtil.Month month = MonthUtil.getMonth(value);
        if (month.isValid()) {
            return month.bibtexFormat;
        } else {
            return value;
        }
    }
}
