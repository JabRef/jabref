package org.jabref.model.strings;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

import com.github.tomtung.latex2unicode.LaTeX2Unicode;

/**
 * Adapter class for the latex2unicode lib. This is an alternative to our LatexToUnicode class
 */
public class LatexToUnicodeAdapter {

    private static Pattern underscoreMatcher = Pattern.compile("_(?!\\{)");

    private static String replacementChar = "\uFFFD";

    private static Pattern underscorePlaceholderMatcher = Pattern.compile(replacementChar);

    public static String format(String inField) {
        Objects.requireNonNull(inField);

        String toFormat = underscoreMatcher.matcher(inField).replaceAll(replacementChar);
        toFormat = Normalizer.normalize(LaTeX2Unicode.convert(toFormat), Normalizer.Form.NFC);
        return underscorePlaceholderMatcher.matcher(toFormat).replaceAll("_");
    }
}
