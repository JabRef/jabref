package org.jabref.logic.bst.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The |built_in| function {\.{text.prefix\$}} pops the top two literals (the
 * integer literal |pop_lit1| and a string literal, in that order). It pushes
 * the substring of the (at most) |pop_lit1| consecutive text characters
 * starting from the beginning of the string. This function is similar to
 * {\.{substring\$}}, but this one considers an accented character (or more
 * precisely, a ``special character''$\!$, even if it's missing its matching
 * |right_brace|) to be a single text character (rather than however many
 * |ASCII_code| characters it actually comprises), and this function doesn't
 * consider braces to be text characters; furthermore, this function appends any
 * needed matching |right_brace|s. If any of the types is incorrect, it
 * complains and pushes the null string.
 *
 */
public class BstTextPrefixer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BstTextPrefixer.class);

    private BstTextPrefixer() {
    }

    public static String textPrefix(int numOfChars, String toPrefix) {
        StringBuilder sb = new StringBuilder();
        PrefixState prefixState = new PrefixState(0, 0, numOfChars);
        char[] cs = toPrefix.toCharArray();
        while (prefixState.index < cs.length && prefixState.numOfChars > 0) {
            char c = cs[prefixState.index++];
            handleOpeningBrace(cs, prefixState, c);
            handleClosingBrace(prefixState, toPrefix, c);
            boolean isNormalCharacter = c != '{' && c != '}';
            if (isNormalCharacter) {
                prefixState.numOfChars--;
            }
        }
        sb.append(toPrefix, 0, prefixState.index);
        // Append any remaining closing braces if unbalanced
        while (prefixState.braceLevel-- > 0) {
            sb.append('}');
        }
        return sb.toString();
    }

    private static void handleOpeningBrace(char[] cs, PrefixState prefixState, char c) {
        if (c != '{') {
            return;
        }
        prefixState.braceLevel++;
        if ((prefixState.braceLevel == 1) && (prefixState.index < cs.length) && (cs[prefixState.index] == '\\')) {
            prefixState.index++; // skip backslash
            while ((prefixState.index < cs.length) && (prefixState.braceLevel > 0)) {
                if (cs[prefixState.index] == '}') {
                    prefixState.braceLevel--;
                } else if (cs[prefixState.index] == '{') {
                    prefixState.braceLevel++;
                }
                prefixState.index++;
            }
            prefixState.numOfChars--;
        }
    }

    private static void handleClosingBrace(PrefixState prefixState, String toPrefix, char c) {
        if (c != '}') {
            return;
        }
        if (prefixState.braceLevel > 0) {
            prefixState.braceLevel--;
        } else {
            LOGGER.warn("Unbalanced brace in string for purify$: {}", toPrefix);
        }
    }

    private static class PrefixState {
        public int index;
        public int braceLevel;
        public int numOfChars;

        public PrefixState(int index, int braceLevel, int numOfChars) {
            this.index = index;
            this.braceLevel = braceLevel;
            this.numOfChars = numOfChars;
        }
    }
}
