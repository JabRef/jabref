package net.sf.jabref.logic.formatter.casechanger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Parses a title to a list of words.
 */
public final class TitleParser {

    private StringBuffer buffer;
    private int wordStart;

    public List<Word> parse(String title) {
        List<Word> words = new LinkedList<>();

        boolean[] isProtected = determineProtectedChars(title);

        reset();

        int index = 0;
        for (char c : title.toCharArray()) {
            if (Character.isWhitespace(c)) {
                createWord(isProtected).ifPresent(words::add);
            } else {
                if (wordStart == -1) {
                    wordStart = index;
                }

                buffer.append(c);
            }

            index++;
        }
        createWord(isProtected).ifPresent(words::add);

        return words;
    }

    private Optional<Word> createWord(boolean[] isProtected) {
        if (buffer.length() <= 0) {
            return Optional.empty();
        }

        char[] chars = buffer.toString().toCharArray();
        boolean[] protectedChars = new boolean[chars.length];

        System.arraycopy(isProtected, wordStart, protectedChars, 0, chars.length);

        reset();

        return Optional.of(new Word(chars, protectedChars));
    }

    private void reset() {
        wordStart = -1;
        buffer = new StringBuffer();
    }

    private static boolean[] determineProtectedChars(String title) {
        boolean[] isProtected = new boolean[title.length()];
        char[] chars = title.toCharArray();

        int brackets = 0;
        for (int i = 0; i < title.length(); i++) {
            if (chars[i] == '{') {
                brackets++;
            } else if (chars[i] == '}') {
                brackets--;
            } else {
                isProtected[i] = brackets > 0;
            }
        }

        return isProtected;
    }

}
