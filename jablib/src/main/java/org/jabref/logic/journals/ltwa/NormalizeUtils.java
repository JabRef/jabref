package org.jabref.logic.journals.ltwa;

import java.text.Normalizer;
import java.util.Optional;

public final class NormalizeUtils {

    /**
     * Normalizes text using Unicode normalization form NFKC
     * (Compatibility Decomposition, followed by Canonical Composition)
     */
    public static Optional<String> toNFKC(String input) {
        return Optional.ofNullable(input)
                       .map(s -> Normalizer.normalize(s, Normalizer.Form.NFKC));
    }

    /**
     * Normalizes text by removing diacritical marks
     */
    public static Optional<String> normalize(String input) {
        return Optional.ofNullable(input)
                       .map(s -> Normalizer.normalize(s, Normalizer.Form.NFD)
                                           .replaceAll("\\p{InCombiningDiacriticalMarks}+", ""));
    }
}
