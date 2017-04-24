package org.jabref.logic.bibtex;

import org.jabref.logic.util.OS;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.strings.StringUtil;

/**
 * Currently the only implementation of org.jabref.exporter.FieldFormatter
 * <p>
 * Obeys following settings:
 * * JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS
 * * JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR
 * * JabRefPreferences.WRITEFIELD_WRAPFIELD
 */
public class LatexFieldFormatter {

    // "Fieldname" to indicate that a field should be treated as a bibtex string. Used when writing database to file.
    public static final String BIBTEX_STRING = "__string";
    private static final char FIELD_START = '{';
    private static final char FIELD_END = '}';
    private final boolean neverFailOnHashes;
    private final LatexFieldFormatterPreferences prefs;
    private final FieldContentParser parser;
    private StringBuilder stringBuilder;


    public LatexFieldFormatter(LatexFieldFormatterPreferences prefs) {
        this(true, prefs);
    }

    private LatexFieldFormatter(boolean neverFailOnHashes, LatexFieldFormatterPreferences prefs) {
        this.neverFailOnHashes = neverFailOnHashes;
        this.prefs = prefs;

        parser = new FieldContentParser(prefs.getFieldContentParserPreferences());
    }

    public static LatexFieldFormatter buildIgnoreHashes(LatexFieldFormatterPreferences prefs) {
        return new LatexFieldFormatter(true, prefs);
    }

    private static void checkBraces(String text) throws InvalidFieldValueException {
        int left = 0;
        int right = 0;

        // First we collect all occurrences:
        for (int i = 0; i < text.length(); i++) {
            char item = text.charAt(i);

            boolean charBeforeIsEscape = false;
            if (i > 0 && text.charAt(i - 1) == '\\') {
                charBeforeIsEscape = true;
            }

            if (!charBeforeIsEscape && item == '{') {
                left++;
            } else if (!charBeforeIsEscape && item == '}') {
                right++;
            }
        }

        // Then we throw an exception if the error criteria are met.
        if (!(right == 0) && (left == 0)) {
            throw new InvalidFieldValueException("Unescaped '}' character without opening bracket ends string prematurely.");
        }
        if (!(right == 0) && (right < left)) {
            throw new InvalidFieldValueException("Unescaped '}' character without opening bracket ends string prematurely.");
        }
        if (left != right) {
            throw new InvalidFieldValueException("Braces don't match.");
        }
    }

    /**
     * Formats the content of a field.
     *
     * @param content   the content of the field
     * @param fieldName the name of the field - used to trigger different serializations, e.g., turning off resolution for some strings
     * @return a formatted string suitable for output
     * @throws InvalidFieldValueException if s is not a correct bibtex string, e.g., because of improperly balanced braces or using # not paired
     */
    public String format(String content, String fieldName) throws InvalidFieldValueException {

        if (content == null) {
            return FIELD_START + String.valueOf(FIELD_END);
        }

        String result = content;

        // normalize newlines
        boolean shouldNormalizeNewlines = !result.contains(OS.NEWLINE) && result.contains("\n");
        if (shouldNormalizeNewlines) {
            // if we don't have real new lines, but pseudo newlines, we replace them
            // On Win 8.1, this is always true for multiline fields
            result = result.replace("\n", OS.NEWLINE);
        }

        // If the field is non-standard, we will just append braces,
        // wrap and write.
        boolean resolveStrings = shouldResolveStrings(fieldName);

        if (!resolveStrings) {
            return formatWithoutResolvingStrings(result, fieldName);
        }

        // Trim whitespace
        result = result.trim();
        return formatAndResolveStrings(result, fieldName);
    }

    /**
     * This method handles # in the field content to get valid bibtex strings
     *
     * For instance, <code>#jan# - #feb#</code> gets  <code>jan #{ - } # feb</code> (see @link{org.jabref.logic.bibtex.LatexFieldFormatterTests#makeHashEnclosedWordsRealStringsInMonthField()})
     */
    private String formatAndResolveStrings(String content, String fieldName) throws InvalidFieldValueException {
        stringBuilder = new StringBuilder();
        checkBraces(content);

        // Here we assume that the user encloses any bibtex strings in #, e.g.:
        // #jan# - #feb#
        // ...which will be written to the file like this:
        // jan # { - } # feb
        int pivot = 0;
        while (pivot < content.length()) {
            int goFrom = pivot;
            int pos1 = pivot;
            while (goFrom == pos1) {
                pos1 = content.indexOf('#', goFrom);
                if ((pos1 > 0) && (content.charAt(pos1 - 1) == '\\')) {
                    goFrom = pos1 + 1;
                    pos1++;
                } else {
                    goFrom = pos1 - 1; // Ends the loop.
                }
            }

            int pos2;
            if (pos1 == -1) {
                pos1 = content.length(); // No more occurrences found.
                pos2 = -1;
            } else {
                pos2 = content.indexOf('#', pos1 + 1);
                if (pos2 == -1) {
                    if (neverFailOnHashes) {
                        pos1 = content.length(); // just write out the rest of the text, and throw no exception
                    } else {
                        throw new InvalidFieldValueException(
                                "The # character is not allowed in BibTeX strings unless escaped as in '\\#'.\n"
                                        + "In JabRef, use pairs of # characters to indicate a string.\n"
                                        + "Note that the entry causing the problem has been selected.");
                    }
                }
            }

            if (pos1 > pivot) {
                writeText(content, pivot, pos1);
            }
            if ((pos1 < content.length()) && ((pos2 - 1) > pos1)) {
                // We check that the string label is not empty. That means
                // an occurrence of ## will simply be ignored. Should it instead
                // cause an error message?
                writeStringLabel(content, pos1 + 1, pos2, pos1 == pivot,
                        (pos2 + 1) == content.length());
            }

            if (pos2 > -1) {
                pivot = pos2 + 1;
            } else {
                pivot = pos1 + 1;
            }
        }

        return parser.format(stringBuilder, fieldName);
    }

    private boolean shouldResolveStrings(String fieldName) {
        boolean resolveStrings = true;
        if (prefs.isResolveStringsAllFields()) {
            // Resolve strings for all fields except some:
            for (String exception : prefs.getDoNotResolveStringsFor()) {
                if (exception.equals(fieldName)) {
                    resolveStrings = false;
                    break;
                }
            }
        } else {
            // Default operation - we only resolve strings for standard fields:
            resolveStrings = InternalBibtexFields.isStandardField(fieldName)
                    || BIBTEX_STRING.equals(fieldName);
        }
        return resolveStrings;
    }

    private String formatWithoutResolvingStrings(String content, String fieldName) throws InvalidFieldValueException {
        checkBraces(content);

        stringBuilder = new StringBuilder(
                String.valueOf(FIELD_START));

        stringBuilder.append(parser.format(content, fieldName));

        stringBuilder.append(FIELD_END);

        return stringBuilder.toString();
    }

    private void writeText(String text, int startPos, int endPos) {

        stringBuilder.append(FIELD_START);
        boolean escape = false;
        boolean inCommandName = false;
        boolean inCommand = false;
        boolean inCommandOption = false;
        int nestedEnvironments = 0;
        StringBuilder commandName = new StringBuilder();
        for (int i = startPos; i < endPos; i++) {
            char c = text.charAt(i);

            // Track whether we are in a LaTeX command of some sort.
            if (Character.isLetter(c) && (escape || inCommandName)) {
                inCommandName = true;
                if (!inCommandOption) {
                    commandName.append(c);
                }
            } else if (Character.isWhitespace(c) && (inCommand || inCommandOption)) {
                // Whitespace
            } else if (inCommandName) {
                // This means the command name is ended.
                // Perhaps the beginning of an argument:
                if (c == '[') {
                    inCommandOption = true;
                } else if (inCommandOption && (c == ']')) {
                    // Or the end of an argument:
                    inCommandOption = false;
                } else if (!inCommandOption && (c == '{')) {
                    inCommandName = false;
                    inCommand = true;
                } else {
                    // Or simply the end of this command alltogether:
                    commandName.delete(0, commandName.length());
                    inCommandName = false;
                }
            }
            // If we are in a command body, see if it has ended:
            if (inCommand && (c == '}')) {
                if ("begin".equals(commandName.toString())) {
                    nestedEnvironments++;
                }
                if ((nestedEnvironments > 0) && "end".equals(commandName.toString())) {
                    nestedEnvironments--;
                }

                commandName.delete(0, commandName.length());
                inCommand = false;
            }

            // We add a backslash before any ampersand characters, with one exception: if
            // we are inside an \\url{...} command, we should write it as it is. Maybe.
            if ((c == '&') && !escape && !(inCommand && "url".equals(commandName.toString()))
                    && (nestedEnvironments == 0)) {
                stringBuilder.append("\\&");
            } else {
                stringBuilder.append(c);
            }
            escape = c == '\\';
        }
        stringBuilder.append(FIELD_END);
    }

    private void writeStringLabel(String text, int startPos, int endPos,
                                  boolean first, boolean last) {
        putIn((first ? "" : " # ") + text.substring(startPos, endPos)
                + (last ? "" : " # "));
    }

    private void putIn(String s) {
        stringBuilder.append(StringUtil.wrap(s, prefs.getLineLength(), OS.NEWLINE));
    }

}
