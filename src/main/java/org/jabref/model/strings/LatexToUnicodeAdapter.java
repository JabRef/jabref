package org.jabref.model.strings;

import java.text.Normalizer;
import java.util.Objects;

import com.github.tomtung.latex2unicode.LaTeX2Unicode;

/**
 * Adapter class for the latex2unicode lib. This is an alternative to our LatexToUnicode class
 */
public class LatexToUnicodeAdapter {

    public static String format(String inField) {
        Objects.requireNonNull(inField);

        return Normalizer.normalize(LaTeX2Unicode.convert(inField), Normalizer.Form.NFKC);
    }
}
