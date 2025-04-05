package org.jabref.logic.journals.ltwa;

import java.io.Serializable;
import java.util.List;

/**
 * Represents an entry in the LTWA (List of Title Word Abbreviations).
 * Each entry contains a word, its abbreviation, and the languages it applies
 * to.
 *
 * @param word         The word to be abbreviated
 * @param abbreviation The abbreviation for the word
 * @param languages    A list of language codes that the abbreviation applies to
 *                     If "mul" is present, it indicates that the abbreviation
 *                     applies to multiple languages.
 *                     Otherwise, it contains specific language codes (e.g.,
 *                     "eng", "fre", "deu").
 */
public record LtwaEntry(
        String word,
        String abbreviation,
        List<String> languages) implements Serializable {
}
