package net.sf.jabref.model.strings;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

import com.github.tomtung.latex2unicode.DefaultLatexToUnicodeConverter;

/**
 * Adapter class for the latex2unicode lib. This is an alternative to our LatexToUnicode class
 */
public class LatexToUnicodeAdapter {

    /**
     * Matches a '~' as long as it is not between '\' and '{'.
     *
     * Should match the normal ~ that people enter for getting a whitespace between connected things
     */
    private static final Pattern TILDE = Pattern.compile("(?<!\\\\)~(?<!\\{)");

    public static String format(String inField) {
        Objects.requireNonNull(inField);

        String result = TILDE.matcher(inField).replaceAll("\u00A0");
        result = Normalizer.normalize(DefaultLatexToUnicodeConverter.convert(result).toString(), Normalizer.Form.NFC);

        return result;
    }
}
