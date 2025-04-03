package org.jabref.logic.journals.ltwa;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class NormalizeUtils {
    public static final Pattern BOUNDARY = Pattern.compile("[-\\s\\u2013\\u2014_.,:;!|=+*\\\\/\"()&#%@$?]");

    public static String toNFKC(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFKC);
    }

    public static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
