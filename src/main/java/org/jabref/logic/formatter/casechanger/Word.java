package org.jabref.logic.formatter.casechanger;

import com.sun.star.lang.IllegalArgumentException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a word in a title of a bibtex entry.
 * <p>
 * A word can have protected chars (enclosed in '{' '}') and may be a small (a, an, the, ...) word.
 */
public final class Word {
    /**
     * Set containing common lowercase function words
     */
    public static final Set<String> SMALLER_WORDS;
    private final char[] chars;
    private final boolean[] protectedChars;

    static {
        Set<String> smallerWords = new HashSet<>();

        // Articles
        smallerWords.addAll(Arrays.asList("a", "an", "the"));
        // Prepositions
        smallerWords.addAll(Arrays.asList("above", "about", "across", "against", "along", "among", "around", "at", "before", "behind", "below", "beneath", "beside", "between", "beyond", "by", "down", "during", "except", "for", "from", "in", "inside", "into", "like", "near", "of", "off", "on", "onto", "since", "to", "toward", "through", "under", "until", "up", "upon", "with", "within", "without"));
        // Conjunctions
        smallerWords.addAll(Arrays.asList("and", "but", "for", "nor", "or", "so", "yet"));

        // unmodifiable for thread safety
        SMALLER_WORDS = smallerWords.stream()
                            .map(word -> word.toLowerCase(Locale.ROOT))
                            .collect(Collectors.toUnmodifiableSet());
    }

    public Word(char[] chars, boolean[] protectedChars) {
        this.chars = Objects.requireNonNull(chars);
        this.protectedChars = Objects.requireNonNull(protectedChars);

        if (this.chars.length != this.protectedChars.length) {
            throw new IllegalArgumentException("the chars and the protectedChars array must be of same length");
        }
    }

    /**
     * Case-insensitive check against {@link Word#SMALLER_WORDS}. Checks for common function words.
     */
    public static boolean isSmallerWord(String word) {
        return SMALLER_WORDS.contains(word.toLowerCase(Locale.ROOT));
    }

    /**
     * Only change letters of the word that are unprotected to upper case.
     */
    public void toUpperCase() {
        for (int i = 0; i < chars.length; i++) {
            if (!protectedChars[i]) {
                chars[i] = Character.toUpperCase(chars[i]);
            }
        }
    }

    /**
     * Only change letters of the word that are unprotected to lower case.
     */
    public void toLowerCase() {
        for (int i = 0; i < chars.length; i++) {
            if (!protectedChars[i]) {
                chars[i] = Character.toLowerCase(chars[i]);
            }
        }
    }

    public void toUpperFirst() {
        for (int i = 0; i < chars.length; i++) {
            if (!protectedChars[i]) {
                if (i == 0) {
                    chars[i] = Character.toUpperCase(chars[i]);
                }
                else if (hasAnyDash(chars[i])) {
                    chars[i+1] = Character.toUpperCase(chars[i+1]);
                    i++;
                }
                else {
                    chars[i] = Character.toLowerCase(chars[i]);
                }
            }
        }
    }

    public boolean isSmallerWord() {
        // "word:" is still a small "word"
        return SMALLER_WORDS.contains(this.toString().replace(":", "").toLowerCase(Locale.ROOT));
    }

    public boolean isLargerWord() {
        return !isSmallerWord();
    }

    @Override
    public String toString() {
        return new String(chars);
    }

    public boolean endsWithColon() {
        return this.toString().endsWith(":");
    }

    public boolean hasAnyDash(char c) {
        char[] dashes = {'\u002D', '\u058A', '\u05BE', '\u1400', '\u1806', '\u2010',
                '\u2011', '\u2012', '\u2013', '\u2014', '\u2015', '\u2E17',
                '\u2E1A', '\u2E3A', '\u2E3B', '\u2E40', '\u301C', '\u3030',
                '\u30A0', '\uFE31', '\uFE32', '\uFE58', '\uFE63', '\uFF0D'};

        for (char x : dashes) {
            if (x == c) {
                return true;
            }
        }
        return false;
    }
}
