package net.sf.jabref.logic.formatter.casechanger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a word in a title of a bibtex entry.
 * <p>
 * A word can have protected chars (enclosed in '{' '}') and may be a small (a, an, the, ...) word.
 */
public final class Word {
    public static final Set<String> SMALLER_WORDS;

    static {
        Set<String> smallerWords = new HashSet<>();

        // Articles
        smallerWords.addAll(Arrays.asList("a", "an", "the"));
        // Prepositions
        smallerWords.addAll(Arrays.asList("above", "about", "across", "against", "along", "among", "around", "at", "before", "behind", "below", "beneath", "beside", "between", "beyond", "by", "down", "during", "except", "for", "from", "in", "inside", "into", "like", "near", "of", "off", "on", "onto", "since", "to", "toward", "through", "under", "until", "up", "upon", "with", "within", "without"));
        // Conjunctions
        smallerWords.addAll(Arrays.asList("and", "but", "for", "nor", "or", "so", "yet"));

        // unmodifiable for thread safety
        SMALLER_WORDS = Collections.unmodifiableSet(smallerWords);
    }

    private final char[] chars;
    private final boolean[] protectedChars;

    public Word(char[] chars, boolean[] protectedChars) {
        this.chars = Objects.requireNonNull(chars);
        this.protectedChars = Objects.requireNonNull(protectedChars);

        if (this.chars.length != this.protectedChars.length) {
            throw new IllegalArgumentException("the chars and the protectedChars array must be of same length");
        }
    }

    /**
     * Only change letters of the word that are unprotected to upper case.
     */
    public void toUpperCase() {
        for (int i = 0; i < chars.length; i++) {
            if (protectedChars[i]) {
                continue;
            }

            chars[i] = Character.toUpperCase(chars[i]);
        }
    }

    /**
     * Only change letters of the word that are unprotected to lower case.
     */
    public void toLowerCase() {
        for (int i = 0; i < chars.length; i++) {
            if (protectedChars[i]) {
                continue;
            }

            chars[i] = Character.toLowerCase(chars[i]);
        }
    }


    public void toUpperFirst() {
        for (int i = 0; i < chars.length; i++) {
            if (protectedChars[i]) {
                continue;
            }

            if (i == 0) {
                chars[i] = Character.toUpperCase(chars[i]);
            } else {
                chars[i] = Character.toLowerCase(chars[i]);
            }
        }
    }

    public boolean isSmallerWord() {
        // "word:" is still a small "word"
        return SMALLER_WORDS.contains(this.toString().replace(":", "").toLowerCase());
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
}
