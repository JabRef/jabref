package org.jabref.logic.formatter.bibtexfields;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.entry.Month;
import org.jabref.model.strings.LatexToUnicodeAdapter;

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
        Optional<Month> month = Month.parse(value);

        if (!month.isPresent()) {
            // Normalize month name to unicode, which is returned by DateFormatSymbols.getMonths() - see below
            value = new LatexToUnicodeAdapter().format(value);

            if (value.equalsIgnoreCase("Maerz")) {
                // this value is not treated by LatexToUnicodeAdapter, so a special handling is required.
                month = Month.getMonthByNumber(3);
            } else {
                Locale[] availableLocales = Locale.getAvailableLocales();
                int i = 0;
                do {
                    String[] months = new DateFormatSymbols(availableLocales[i]).getMonths();
                    int j = 0;
                    do {
                        if (value.equalsIgnoreCase(months[j])) {
                            month = Month.getMonthByNumber(j + 1);
                        }
                        j++;
                    } while (!month.isPresent() && (j < months.length));
                    i++;
                } while (!month.isPresent() && (i < availableLocales.length));
            }
        }

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
