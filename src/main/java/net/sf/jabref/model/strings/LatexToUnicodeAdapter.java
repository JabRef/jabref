package net.sf.jabref.model.strings;

import java.text.Normalizer;
import java.util.Objects;

import com.github.tomtung.latex2unicode.DefaultLatexToUnicodeConverter;

/**
 * Adapter class for the latex2unicode lib. This is an alternative to our LatexToUnicode class
 */
public class LatexToUnicodeAdapter {

    public static String format(String inField) {
        Objects.requireNonNull(inField);

        String result = Normalizer.normalize(DefaultLatexToUnicodeConverter.convert(inField).toString(), Normalizer.Form.NFC);

        return result;
    }
}
