package org.jabref.logic.util.strings;

import org.jabref.logic.formatter.casechanger.CapitalizeFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.model.util.ResultingStringState;

public class StringManipulator {
    private enum LetterCase {
        UPPER,
        LOWER,
        CAPITALIZED
    }

    private enum Direction {
        NEXT, PREVIOUS
    }

    private static ResultingStringState setWordCase(String text, int pos, LetterCase targetCase) {
        StringBuilder result = new StringBuilder();
        int i = pos;

        // swallow whitespaces
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            result.append(text.charAt(i));
            i++;
        }
        if (i >= text.length()) {
            return new ResultingStringState(i, text);
        }

        StringBuilder nextWord = new StringBuilder();
        // read next word
        char currentChar = text.charAt(i);
        while ((Character.isLetterOrDigit(currentChar) || Character.toString(currentChar).equals("_"))) {
            nextWord.append(currentChar);
            i++;

            if (i >= text.length()) {
                break;
            }
            currentChar = text.charAt(i);
        }

        String changedString = switch (targetCase) {
            case UPPER -> (new UpperCaseFormatter()).format(nextWord.toString());
            case LOWER -> (new LowerCaseFormatter()).format(nextWord.toString());
            case CAPITALIZED -> (new CapitalizeFormatter()).format(nextWord.toString());
        };

        String res = text.substring(0, pos) + result.toString() + changedString + text.substring(i);
        return new ResultingStringState(i, res);
    }

    /**
     * Delete all characters in a string from the given position to the next word boundary.
     *
     * @param pos The index to start from.
     * @param text The text to manipulate.
     * @param dir The direction to search.
     * @return The resulting text and caret position.
     */
    public static ResultingStringState deleteUntilWordBoundary(int pos, String text, Direction dir) {
        StringBuilder res = new StringBuilder();
        int offset;
        int wordBreak;
        switch (dir) {
            case NEXT -> {
                res.append(text, 0, pos);
                offset = 1;
                wordBreak = text.length();
            }
            case PREVIOUS -> {
                res.append(text, pos, text.length());
                offset = -1;
                wordBreak = 0;
            }
            default -> throw new AssertionError("Missing case in switch deleteUntilWordBoundary");
        }

        for (int i = pos; i < text.length() && i >= 0; i += offset) {
            if (i == pos) {
                // Swallow whitespace until we hit a word character or newline.
                while (i < text.length()
                        && i >= 0
                        && !String.valueOf(text.charAt(i)).matches("\\w|[\\r\\n]")) {
                    i += offset;
                }
            }
            if (!(i < text.length() && i >= 0) || Character.isWhitespace(text.charAt(i))) {
                wordBreak = i;
                break;
            }
        }
        int caretPosition;
        if (dir == Direction.NEXT) {
            res.append(text, wordBreak, text.length());
            // Since we deleted forward, we're in the right place already.
            caretPosition = pos;

        } else {
            // Since we deleted backwards, we need to move the caret appropriately.
            // We need to protect against having stepped beyond the string during the while-loop.
            if (wordBreak != -1) {
                res.append(text, 0, wordBreak);
                caretPosition = wordBreak;
            } else {
                caretPosition = 0;
            }
        }
        return new ResultingStringState(caretPosition, res.toString());
    }

    /**
     * Capitalize the word on the right side of the cursor.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String The resulting text and caret position.
     */
    public static ResultingStringState capitalize(int pos, String text) {
        return setWordCase(text, pos, LetterCase.CAPITALIZED);
    }

    /**
     * Make all characters in the word uppercase.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String The resulting text and caret position.
     */
    public static ResultingStringState uppercase(int pos, String text) {
        return setWordCase(text, pos, LetterCase.UPPER);
    }

    /**
     * Make all characters in the word lowercase.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String The resulting text and caret position.
     */
    public static ResultingStringState lowercase(int pos, String text) {
        return setWordCase(text, pos, LetterCase.LOWER);
    }

    /**
     * Remove the next word on the right side of the cursor.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String The resulting text and caret position.
     */
    public static ResultingStringState killWord(int pos, String text) {
        return deleteUntilWordBoundary(pos, text, Direction.NEXT);
    }

    /**
     * Remove the previous word on the left side of the cursor.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String The resulting text and caret position.
     */
    public static ResultingStringState backwardKillWord(int pos, String text) {
        return deleteUntilWordBoundary(pos, text, Direction.PREVIOUS);
    }
}
