package org.jabref.logic.journals.ltwa;

import java.text.Normalizer;
import java.util.Optional;

public class NormalizeUtils {
    public static Optional<String> toNFKC(String input) {
        return Optional.ofNullable(input)
                       .map(s -> Normalizer.normalize(s, Normalizer.Form.NFKC));
    }

    public static Optional<String> normalize(String input) {
        return Optional.ofNullable(input)
                       .map(s -> Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", ""));
    }
}
