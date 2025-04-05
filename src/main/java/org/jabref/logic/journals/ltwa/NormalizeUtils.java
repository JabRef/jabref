package org.jabref.logic.journals.ltwa;

import java.text.Normalizer;

public class NormalizeUtils {
    public static String toNFKC(String input) {
        if (input == null) {
            return null;
        }
        return Normalizer.normalize(input, Normalizer.Form.NFKC);
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
