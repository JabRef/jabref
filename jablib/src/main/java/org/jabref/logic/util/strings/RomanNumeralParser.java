package org.jabref.logic.util.strings;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RomanNumeralParser {

    // Roman Numbers Map
    private static final Map<Character, Integer> ROMAN_VALUES = Map.of(
            'I', 1,
            'V', 5,
            'X', 10,
            'L', 50,
            'C', 100,
            'D', 500,
            'M', 1000);
    private static final Pattern ROMAN_PATTERN = Pattern.compile("M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})");

    public static Optional<Integer> parse(String romanText) {
        // Normalize the roman number
        if (romanText == null) {
            return Optional.empty();
        }

        String normalizedRomanText = romanText.trim().toUpperCase(Locale.ROOT);
        if (normalizedRomanText.isEmpty()) {
            return Optional.empty();
        }

        // Check if the string is valid Roman Number
        if (RomanNumeralParser.isValid(normalizedRomanText)) {
            int len = normalizedRomanText.length();
            int result = 0;

            for (int i = 0; i < len; i++) {
                char ch1 = normalizedRomanText.charAt(i);
                int value = ROMAN_VALUES.get(ch1);
                int next;
                if (i + 1 == len) {
                    next = 0;
                } else {
                    char ch2 = normalizedRomanText.charAt(i + 1);
                    next = ROMAN_VALUES.get(ch2);
                }

                if (value < next) {
                    result -= value;
                } else {
                    result += value;
                }
            }

            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    public static boolean isValid(String str) {
        if (str == null) {
            return false;
        }

        Matcher m = ROMAN_PATTERN.matcher(str);

        // Return if the str matched the REGEX
        return m.matches();
    }
}
