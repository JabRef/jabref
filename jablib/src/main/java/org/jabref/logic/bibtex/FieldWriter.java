package org.jabref.logic.bibtex;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringJoiner;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts JabRef's internal BibTeX representation of a BibTeX field to BibTeX text representation
 */
public class FieldWriter {

    // See also ADR-0024
    public static final char BIBTEX_STRING_START_END_SYMBOL = '#';

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldWriter.class);

    private static final char FIELD_START = '{';
    private static final char FIELD_END = '}';

    private final boolean neverFailOnHashes;
    private final FieldPreferences preferences;

    public FieldWriter(FieldPreferences preferences) {
        this(true, preferences);
    }

    private FieldWriter(boolean neverFailOnHashes, FieldPreferences preferences) {
        this.neverFailOnHashes = neverFailOnHashes;
        this.preferences = preferences;
    }

    public static FieldWriter buildIgnoreHashes(FieldPreferences prefs) {
        return new FieldWriter(true, prefs);
    }

    private static void checkBraces(String text) throws InvalidFieldValueException {
        Deque<String> queue = new ArrayDeque<>();
        int line = 0;
        int lastLineIndex = 0;

        // First we collect all occurrences:
        for (int i = 0; i < text.length(); i++) {
            char item = text.charAt(i);
            if (item == '\n') {
                line++;
                lastLineIndex = i;
                continue;
            }

            if (!isEscaped(text, i)) {
                if (item == '{') {
                    queue.add("Line %d, column %d (index %d): in '%s'".formatted(
                            line + 1, i - lastLineIndex + 1, i, getErrorContextSnippet(text, i)));
                } else if (item == '}') {
                    if (queue.pollLast() == null) {
                        String errorMessage = "Unescaped '}' without matching opening bracket found at line %d, column %d (index %d): in '%s'".formatted(
                                line + 1, i - lastLineIndex + 1, i, getErrorContextSnippet(text, i));
                        LOGGER.error(errorMessage);
                        throw new InvalidFieldValueException(errorMessage);
                    }
                }
            }
        }

        if (!queue.isEmpty()) {
            StringJoiner joiner = new StringJoiner("\n");
            for (String error : queue) {
                joiner.add(error);
            }
            String errorMessage = "The following unescaped '{' do not have matching closing bracket:\n%s".formatted(joiner);
            LOGGER.error(errorMessage);
            throw new InvalidFieldValueException(errorMessage);
        }
    }

    private static String getErrorContextSnippet(String text, int index) {
        int neighbourSize = 5;
        return text.substring(Math.max(0, index - neighbourSize), index)
                + "*" + text.charAt(index) + "*"
                + text.substring(index + 1, Math.min(text.length(), index + neighbourSize + 1));
    }

    /**
     * Checks if the character at the specified index in the given text is escaped.
     * A character is considered escaped if it is preceded by an odd number of backslashes (\).
     *
     * @param text  the input string to check for escaped characters
     * @param index the index of the character in the text to check for escaping
     * @return true if the character at the specified index is escaped, false otherwise
     */
    private static boolean isEscaped(String text, int index) {
        int indexCounter = 0;
        for (int i = index - 1; i >= 0; i--) {
            if (text.charAt(i) == '\\') {
                indexCounter++;
            } else {
                break;
            }
        }
        return indexCounter % 2 == 1;
    }

    /**
     * Formats the content of a field.
     *
     * @param field   the name of the field - used to trigger different serializations, e.g., turning off resolution for some strings
     * @param content the content of the field
     * @return a formatted string suitable for output
     * @throws InvalidFieldValueException if content is not a correct bibtex string, e.g., because of improperly balanced braces or using # not paired
     */
    public String write(Field field, String content) throws InvalidFieldValueException {
        if (content == null) {
            return FIELD_START + "" + FIELD_END;
        }

        if (!shouldResolveStrings(field) || field.equals(InternalField.BIBTEX_STRING)) {
            return formatWithoutResolvingStrings(content);
        }

        return formatAndResolveStrings(content);
    }

    /**
     * This method handles # in the field content to get valid bibtex strings
     * <p>
     * For instance, <code>#jan# - #feb#</code> gets  <code>jan #{ - } # feb</code> (see @link{org.jabref.logic.bibtex.LatexFieldFormatterTests#makeHashEnclosedWordsRealStringsInMonthField()})
     */
    private String formatAndResolveStrings(String content) throws InvalidFieldValueException {
        checkBraces(content);

        content = content.replace("##", "");

        StringBuilder stringBuilder = new StringBuilder();

        // Here we assume that the user encloses any bibtex strings in #, e.g.:
        // #jan# - #feb#
        // ...which will be written to the file like this:
        // jan # { - } # feb
        int pivot = 0;
        while (pivot < content.length()) {
            int pos1 = getFirstOccurrenceOfStartEndSymbol(content, pivot);
            int pos2;
            if (pos1 == -1) {
                // Process content and end the loop after that
                pos1 = content.length();
                pos2 = -1;
            } else {
                pos2 = content.indexOf(BIBTEX_STRING_START_END_SYMBOL, pos1 + 1);
            }

            if (pos2 == -1) {
                if (neverFailOnHashes) {
                    pos1 = content.length(); // just write out the rest of the text, and throw no exception
                } else {
                    LOGGER.error("The character {} is not allowed in BibTeX strings unless escaped as in '\\\\{}'. "
                                    + "In JabRef, use pairs of # characters to indicate a string. "
                                    + "Note that the entry causing the problem has been selected. Field value: {}",
                            BIBTEX_STRING_START_END_SYMBOL,
                            BIBTEX_STRING_START_END_SYMBOL,
                            content);
                    throw new InvalidFieldValueException(
                            "The character " + BIBTEX_STRING_START_END_SYMBOL + " is not allowed in BibTeX strings unless escaped as in '\\" + BIBTEX_STRING_START_END_SYMBOL + "'.\n"
                                    + "In JabRef, use pairs of # characters to indicate a string.\n"
                                    + "Note that the entry causing the problem has been selected. Field value: " + content);
                }
            }

            if (pos1 > pivot) {
                writeText(stringBuilder, content, pivot, pos1);
            }
            if ((pos1 < content.length()) && ((pos2 - 1) > pos1)) {
                // We check that the string label is not empty. That means
                // an occurrence of ## will simply be ignored. Should it instead
                // cause an error message?
                writeStringLabel(stringBuilder, content, pos1 + 1, pos2, pos1 == pivot,
                        (pos2 + 1) == content.length());
            }

            if (pos2 > -1) {
                pivot = pos2 + 1;
            } else {
                pivot = pos1 + 1;
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Finds the first occurrence of # from the pivot point
     */
    private static int getFirstOccurrenceOfStartEndSymbol(String content, int pivot) {
        int goFrom = pivot;
        int pos1 = pivot;
        while (goFrom == pos1) {
            pos1 = content.indexOf(BIBTEX_STRING_START_END_SYMBOL, goFrom);
            if ((pos1 > 0) && (content.charAt(pos1 - 1) == '\\')) {
                pos1++;
                goFrom = pos1;
            } else {
                break;
            }
        }
        return pos1;
    }

    private boolean shouldResolveStrings(Field field) {
        if (preferences.shouldResolveStrings()) {
            // Resolve strings for the list of fields only
            return preferences.getResolvableFields().contains(field);
        }
        return false;
    }

    private String formatWithoutResolvingStrings(String content) throws InvalidFieldValueException {
        checkBraces(content);
        return FIELD_START + content + FIELD_END;
    }

    /**
     * @param stringBuilder the StringBuilder to append the text to
     * @param text          the text to append
     */
    private void writeText(StringBuilder stringBuilder, String text, int startPos, int endPos) {
        stringBuilder.append(FIELD_START);
        stringBuilder.append(text, startPos, endPos);
        stringBuilder.append(FIELD_END);
    }

    /**
     * @param stringBuilder the StringBuilder to append the text to
     * @param text          the text use as basis to get the text to append
     * @param startPos      the position in text where the text to add starts
     * @param endPos        the position in text where the text to add ends
     * @param isFirst       true if the label to write is the first one to write
     * @param isLast        true if the label to write is the last one to write
     */
    private void writeStringLabel(StringBuilder stringBuilder, String text, int startPos, int endPos, boolean isFirst, boolean isLast) {
        String line = (isFirst ? "" : " # ") + text.substring(startPos, endPos) + (isLast ? "" : " # ");
        stringBuilder.append(line);
    }
}
