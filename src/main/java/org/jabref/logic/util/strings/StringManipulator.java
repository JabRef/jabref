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

    enum Direction {
        NEXT(1),
        PREVIOUS(-1);

        public final int OFFSET;

        Direction(int offset) {
            this.OFFSET = offset;
        }
    }

    /**
     * Change word casing in a string from the given position to the next word boundary.
     *
     * @param text          The text to manipulate.
     * @param caretPosition The index to start from.
     * @param targetCase    The case mode the string should be changed to.
     *
     * @return              The resulting text and caret position.
     */
    private static ResultingStringState setWordCase(String text, int caretPosition, LetterCase targetCase) {
        int nextWordBoundary = getNextWordBoundary(caretPosition, text, Direction.NEXT);

        // Preserve whitespaces
        int wordStartPosition = caretPosition;
        while (wordStartPosition < nextWordBoundary && Character.isWhitespace(text.charAt(wordStartPosition))) {
            wordStartPosition++;
        }

        String result = switch (targetCase) {
            case UPPER -> (new UpperCaseFormatter()).format(text.substring(wordStartPosition, nextWordBoundary));
            case LOWER -> (new LowerCaseFormatter()).format(text.substring(wordStartPosition, nextWordBoundary));
            case CAPITALIZED -> (new CapitalizeFormatter()).format(text.substring(wordStartPosition, nextWordBoundary));
        };

        return new ResultingStringState(
                nextWordBoundary,
                text.substring(0, wordStartPosition) + result + text.substring(nextWordBoundary));
    }

    /**
     * Delete all characters in a string from the given position to the next word boundary.
     *
     * @param caretPosition The index to start from.
     * @param text          The text to manipulate.
     * @param direction     The direction to search.
     *
     * @return              The resulting text and caret position.
     */
    static ResultingStringState deleteUntilWordBoundary(int caretPosition, String text, Direction direction) {
        // Define cutout range
        int nextWordBoundary = getNextWordBoundary(caretPosition, text, direction);

        // Construct new string without cutout
        return switch (direction) {
            case NEXT -> new ResultingStringState(
                    caretPosition,
                    text.substring(0, caretPosition) + text.substring(nextWordBoundary));
            case PREVIOUS -> new ResultingStringState(
                    nextWordBoundary,
                    text.substring(0, nextWordBoundary) + text.substring(caretPosition));
        };
    }

    /**
     * Utility method to find the next whitespace position in string after text
     * @param caretPosition The current caret Position
     * @param text          The string to search in
     * @param direction     The direction to move through string
     *
     * @return              The position of the next whitespace after a word
     */
    static int getNextWordBoundary(int caretPosition, String text, Direction direction) {
        int i = caretPosition;

        if (direction == Direction.PREVIOUS) {
            // Swallow whitespaces
            while (i > 0 && Character.isWhitespace((text.charAt(i + direction.OFFSET)))) {
                i += direction.OFFSET;
            }

            // Read next word
            while (i > 0 && !Character.isWhitespace(text.charAt(i + direction.OFFSET))) {
                i += direction.OFFSET;
            }
        } else if (direction == Direction.NEXT) {
            // Swallow whitespaces
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i += direction.OFFSET;
            }

            // Read next word
            while (i < text.length() && !Character.isWhitespace((text.charAt(i)))) {
                i += direction.OFFSET;
            }
        }

        return i;
    }

    /**
     * Capitalize the word on the right side of the cursor.
     *
     * @param caretPosition The position of the cursor
     * @param text          The string to manipulate
     *
     * @return String       The resulting text and caret position.
     */
    public static ResultingStringState capitalize(int caretPosition, String text) {
        return setWordCase(text, caretPosition, LetterCase.CAPITALIZED);
    }

    /**
     * Make all characters in the word uppercase.
     *
     * @param caretPosition The position of the cursor
     * @param text          The string to manipulate
     *
     * @return String       The resulting text and caret position.
     */
    public static ResultingStringState uppercase(int caretPosition, String text) {
        return setWordCase(text, caretPosition, LetterCase.UPPER);
    }

    /**
     * Make all characters in the word lowercase.
     *
     * @param caretPosition The position of the cursor
     * @param text          The string to manipulate
     *
     * @return String       The resulting text and caret position.
     */
    public static ResultingStringState lowercase(int caretPosition, String text) {
        return setWordCase(text, caretPosition, LetterCase.LOWER);
    }

    /**
     * Remove the next word on the right side of the cursor.
     *
     * @param caretPosition The position of the cursor
     * @param text          The string to manipulate
     *
     * @return String       The resulting text and caret position.
     */
    public static ResultingStringState killWord(int caretPosition, String text) {
        return deleteUntilWordBoundary(caretPosition, text, Direction.NEXT);
    }

    /**
     * Remove the previous word on the left side of the cursor.
     *
     * @param caretPosition The position of the cursor
     * @param text          The string to manipulate
     *
     * @return String       The resulting text and caret position.
     */
    public static ResultingStringState backwardKillWord(int caretPosition, String text) {
        return deleteUntilWordBoundary(caretPosition, text, Direction.PREVIOUS);
    }
}
