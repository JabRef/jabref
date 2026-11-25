package org.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.Optional;

import org.jabref.logic.util.strings.RomanNumeralParser;
import org.jabref.logic.util.strings.StringUtil;

/// Comparator for numeric cases. The purpose of this class is to add the numeric comparison, because values are sorted
/// as if they were strings.
public class NumericFieldComparator implements Comparator<String> {

    private final boolean parseRomanNumerals;

    public NumericFieldComparator() {
        this(false);
    }

    public NumericFieldComparator(boolean parseRomanNumerals) {
        this.parseRomanNumerals = parseRomanNumerals;
    }

    @Override
    public int compare(String val1, String val2) {
        if ((val1 == null) && (val2 == null)) {
            return 0;
        }

        // Similar implementation as in {@link org.jabref.logic.bibtex.comparator.FieldComparator.compare}.

        Optional<Integer> number1 = toNumber(val1);
        Optional<Integer> number2 = toNumber(val2);

        if (number1.isPresent() && number2.isPresent()) {
            return Integer.compare(number1.get(), number2.get());
        } else if (number1.isPresent()) {
            return 1;
        } else if (number2.isPresent()) {
            return -1;
        } else {
            if (val1 == null) {
             return -1;
        } else if (val2 == null) {
             return 1;
        } else {
             return val1.compareTo(val2);
        }
    }

    }

    private Optional<Integer> toNumber(String value) {
        Optional<Integer> number = StringUtil.intValueOfOptional(value);

        if (number.isEmpty() && parseRomanNumerals) {
            return RomanNumeralParser.parse(value);
        }

        return number;
    }
}
