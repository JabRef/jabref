package org.jabref.logic.util.strings;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;

import com.ibm.icu.text.Transliterator;

public class Transliteration {

    // This transliterator configuration will transliterate from any language to Latin script
    // that uses only allowed characters for citation keys.
    private static final String TRANSLITERATOR_CONFIG = buildTransliteratorConfig();
    private static final Transliterator TRANSLITERATOR = Transliterator.getInstance(TRANSLITERATOR_CONFIG);

    public static String transliterate(String input, boolean removeSpaces) {
        String result = TRANSLITERATOR.transliterate(input);

        // For some reason, icu4j sometimes leaves spaces in the result, so we remove them here if needed.
        if (removeSpaces) {
            result = result.replace(" ", "");
        }

        return result;
    }

    private static String buildTransliteratorConfig() {
        StringBuilder pattern = new StringBuilder();

        for (Character c : CitationKeyGenerator.DISALLOWED_CHARACTERS) {
            // Generally, only characters like `-` or `[` need to be escaped with a backslash,
            // but for future proofing we escape all characters.
            pattern.append("\\").append(c);
        }

        return "Any-Latin; Latin-ASCII; Title; [" + pattern + "] Remove";
    }
}
