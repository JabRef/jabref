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
            if (!Character.isWhitespace(c)) {
                if (wordStart == -1) {
                    wordStart = index;
                }

                buffer.append(c);
            } else {
                createWord(isProtected).ifPresent(words::add);
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

        int brakets = 0;
        for (int i = 0; i < title.length(); i++) {
            if (chars[i] == '{') {
                brakets++;
            } else if (chars[i] == '}') {
                brakets--;
            } else {
                isProtected[i] = brakets > 0;
            }
        }

        return isProtected;
    }

}
