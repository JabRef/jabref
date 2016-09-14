package net.sf.jabref.model.util;


public class ModelStringUtil {

    // Non-letters which are used to denote accents in LaTeX-commands, e.g., in {\"{a}}
    public static final String SPECIAL_COMMAND_CHARS = "\"`^~'=.|";


    public static String booleanToBinaryString(boolean expression) {
        return expression ? "1" : "0";
    }

    /**
     * Quote special characters.
     *
     * @param toQuote         The String which may contain special characters.
     * @param specials  A String containing all special characters except the quoting
     *                  character itself, which is automatically quoted.
     * @param quoteChar The quoting character.
     * @return A String with every special character (including the quoting
     * character itself) quoted.
     */
    public static String quote(String toQuote, String specials, char quoteChar) {
        if (toQuote == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        char c;
        boolean isSpecial;
        for (int i = 0; i < toQuote.length(); ++i) {
            c = toQuote.charAt(i);

            isSpecial = (c == quoteChar);
            // If non-null specials performs logic-or with specials.indexOf(c) >= 0
            isSpecial |= ((specials != null) && (specials.indexOf(c) >= 0));

            if (isSpecial) {
                result.append(quoteChar);
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Creates a substring from a text
     *
     * @param text
     * @param startIndex
     * @param terminateOnEndBraceOnly
     * @return
     */
    public static String getPart(String text, int startIndex, boolean terminateOnEndBraceOnly) {
        char c;
        int count = 0;
    
        StringBuilder part = new StringBuilder();
    
        // advance to first char and skip whitespace
        int index = startIndex + 1;
        while ((index < text.length()) && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    
        // then grab whatever is the first token (counting braces)
        while (index < text.length()) {
            c = text.charAt(index);
            if (!terminateOnEndBraceOnly && (count == 0) && Character.isWhitespace(c)) {
                // end argument and leave whitespace for further processing
                break;
            }
            if ((c == '}') && (--count < 0)) {
                break;
            } else if (c == '{') {
                count++;
            }
            part.append(c);
            index++;
        }
        return part.toString();
    }
}
