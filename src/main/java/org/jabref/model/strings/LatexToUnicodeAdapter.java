package org.jabref.model.strings;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

import com.github.tomtung.latex2unicode.LaTeX2Unicode;
import fastparse.core.Parsed;

/**
 * Adapter class for the latex2unicode lib. This is an alternative to our LatexToUnicode class
 */
public class LatexToUnicodeAdapter {

    private static final Pattern UNDERSCORE_MATCHER = Pattern.compile("_(?!\\{)");

    private static final String REPLACEMENT_CHAR = "\uFFFD";

    private static final Pattern UNDERSCORE_PLACEHOLDER_MATCHER = Pattern.compile(REPLACEMENT_CHAR);

    /**
     * Attempts to resolve all LaTeX in the String.
     *
     * @param inField a String containing LaTeX
     * @return a String with LaTeX resolved into Unicode, or the original String if the LaTeX could not be parsed
     */
    public static String format(String inField) {
        Objects.requireNonNull(inField);

        try {
            return parse(inField);
        } catch (IllegalArgumentException ignored) {
            return Normalizer.normalize(inField, Normalizer.Form.NFC);
        }
    }

    /**
     * Attempts to resolve all LaTeX in the String.
     *
     * @param inField a String containing LaTeX
     * @return a String with LaTeX resolved into Unicode
     * @throws IllegalArgumentException if the LaTeX could not be parsed
     */
    public static String parse(String inField) throws IllegalArgumentException {
        Objects.requireNonNull(inField);
        String toFormat = UNDERSCORE_MATCHER.matcher(inField).replaceAll(REPLACEMENT_CHAR);
        try {
            var parsingResult = LaTeX2Unicode.parse(toFormat);
            if (parsingResult instanceof Parsed.Success) {
                String text = parsingResult.get().value();
                toFormat = Normalizer.normalize(text, Normalizer.Form.NFC);
                return UNDERSCORE_PLACEHOLDER_MATCHER.matcher(toFormat).replaceAll("_");
            } else {
                throw new IllegalArgumentException("Parsing of latex failed.");
            }
        } catch (Throwable throwable) {
            throw new IllegalArgumentException("An error occurred while attempting to parse latex.");
        }
    }
}
