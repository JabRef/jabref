package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

class JavaCharFormatter implements CharFormatter {


    /** Given a character value, return a string representing the character
     * that can be embedded inside a string literal or character literal
     * This works for Java/C/C++ code-generation and languages with compatible
     * special-character-escapment.
     * Code-generators for languages should override this method.
     * @param c   The character of interest.
     * @param forCharLiteral  true to escape for char literal, false for string literal
     */
    public String escapeChar(int c, boolean forCharLiteral) {
        switch (c) {
            //		case GrammarAnalyzer.EPSILON_TYPE : return "<end-of-token>";
            case '\n':
                return "\\n";
            case '\t':
                return "\\t";
            case '\r':
                return "\\r";
            case '\\':
                return "\\\\";
            case '\'':
                return forCharLiteral ? "\\'" : "'";
            case '"':
                return forCharLiteral ? "\"" : "\\\"";
            default :
                if (c < ' ' || c > 126) {
                    if ((0x0000 <= c) && (c <= 0x000F)) {
                        return "\\u000" + Integer.toString(c, 16);
                    }
                    else if ((0x0010 <= c) && (c <= 0x00FF)) {
                        return "\\u00" + Integer.toString(c, 16);
                    }
                    else if ((0x0100 <= c) && (c <= 0x0FFF)) {
                        return "\\u0" + Integer.toString(c, 16);
                    }
                    else {
                        return "\\u" + Integer.toString(c, 16);
                    }
                }
                else {
                    return String.valueOf((char)c);
                }
        }
    }

    /** Converts a String into a representation that can be use as a literal
     * when surrounded by double-quotes.
     * @param s The String to be changed into a literal
     */
    public String escapeString(String s) {
        String retval = new String();
        for (int i = 0; i < s.length(); i++) {
            retval += escapeChar(s.charAt(i), false);
        }
        return retval;
    }

    /** Given a character value, return a string representing the character
     * literal that can be recognized by the target language compiler.
     * This works for languages that use single-quotes for character literals.
     * Code-generators for languages should override this method.
     * @param c   The character of interest.
     */
    public String literalChar(int c) {
        return "'" + escapeChar(c, true) + "'";
    }

    /** Converts a String into a string literal
     * This works for languages that use double-quotes for string literals.
     * Code-generators for languages should override this method.
     * @param s The String to be changed into a literal
     */
    public String literalString(String s) {
        return "\"" + escapeString(s) + "\"";
    }
}
