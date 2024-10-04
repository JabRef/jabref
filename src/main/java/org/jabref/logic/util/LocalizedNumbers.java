package org.jabref.logic.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

public class LocalizedNumbers {
    public static Optional<Double> stringToDouble(String value) {
        return stringToDouble(Locale.getDefault(), value);
    }

    public static Optional<Double> stringToDouble(Locale locale, String value) {
        if (value == null) {
            return Optional.empty();
        }

        try {
            NumberFormat format = NumberFormat.getInstance(locale);
            Number parsedNumber = format.parse(value);
            return Optional.of(parsedNumber.doubleValue());
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    public static String doubleToString(double value) {
        return doubleToString(Locale.getDefault(), value);
    }

    public static String doubleToString(Locale locale, double value) {
        NumberFormat format = NumberFormat.getInstance(locale);
        return format.format(value);
    }
}
