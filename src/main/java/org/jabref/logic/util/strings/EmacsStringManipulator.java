package org.jabref.logic.util.strings;
import org.jabref.model.util.ResultingEmacsState;

public class EmacsStringManipulator {
    private enum LetterCase {
        UPPER,
        LOWER,
        CAPITALIZED
    }

    private enum Direction {
        NEXT, PREVIOUS
    }

    private static ResultingEmacsState setNextWordsCase(String text, int pos, LetterCase targetCase) {
        StringBuilder res = new StringBuilder();

        boolean firstLetter = true;
        int i = pos;
        boolean firstLoop = true;
        StringBuilder newWordBuilder = new StringBuilder();
        for (; i < text.length(); i++) {
            // Swallow whitespace
            while (firstLoop && i < text.length() && !String.valueOf(text.charAt(i)).matches("\\w")) {
                newWordBuilder.append(text.charAt(i));
                i++;
            }
            if (i >= text.length()) {
                break;
            }
            if (firstLoop) {
                firstLoop = false;
            }
            char currentChar = text.charAt(i);
            if (String.valueOf(currentChar).matches("\\w")) {
                switch (targetCase) {
                    case UPPER:
                        newWordBuilder.append(Character.toUpperCase(currentChar));
                        break;
                    case LOWER:
                        newWordBuilder.append(Character.toLowerCase(currentChar));
                        break;
                    case CAPITALIZED:
                        if (firstLetter) {
                            newWordBuilder.append(Character.toUpperCase(currentChar));
                            firstLetter = false;
                        } else {
                            newWordBuilder.append(Character.toLowerCase(currentChar));
                        }
                        break;
                }
            } else {
                // We have reached the word boundary.
                break;
            }
        }
        res.append(text, 0, pos);
        res.append(newWordBuilder);
        res.append(text, i, text.length());

        return new ResultingEmacsState(i, res.toString());
    }

    /**
     * Delete all characters in a string from the given position to the next word boundary.
     *
     * @param pos The index to start from.
     * @param text The text to manipulate.
     * @param dir The direction to search.
     * @return The resulting text.
     */
    public static ResultingEmacsState deleteUntilWordBoundary(int pos, String text, Direction dir) {
        StringBuilder res = new StringBuilder();
        int offset;
        int wordBreak;
        switch (dir) {
            case NEXT:
                res.append(text, 0, pos);
                offset = 1;
                wordBreak = text.length();
                break;
            case PREVIOUS:
                res.append(text, pos, text.length());
                offset = -1;
                wordBreak = 0;
                break;
            default:
                throw new AssertionError("Missing case in switch deleteUntilWordBoundary");
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
            if (!(i < text.length() && i >= 0) || !String.valueOf(text.charAt(i)).matches("\\w")) {
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
        return new ResultingEmacsState(caretPosition, res.toString());
    }

    /**
     * Capitalize the next word on the right side of the cursor.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String the result text
     */
    public static ResultingEmacsState capitalize(int pos, String text) {
        return setNextWordsCase(text, pos, LetterCase.CAPITALIZED);
    }

    /**
     * Make all characters in the next word uppercase.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String the result text
     */
    public static ResultingEmacsState uppercase(int pos, String text) {
        return setNextWordsCase(text, pos, LetterCase.UPPER);
    }

    /**
     * Make all characters in the next word lowercase.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String the result text
     */
    public static ResultingEmacsState lowercase(int pos, String text) {
        return setNextWordsCase(text, pos, LetterCase.LOWER);
    }

    /**
     * Remove the next word on the right side of the cursor.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String the result text
     */
    public static ResultingEmacsState killWord(int pos, String text) {
        return deleteUntilWordBoundary(pos, text, Direction.NEXT);
    }

    /**
     * Remove the previous word on the left side of the cursor.
     *
     * @param pos the position of the cursor
     * @param text String to analyze
     * @return String the result text
     */
    public static ResultingEmacsState backwardKillWord(int pos, String text) {
        return deleteUntilWordBoundary(pos, text, Direction.PREVIOUS);
    }
}
