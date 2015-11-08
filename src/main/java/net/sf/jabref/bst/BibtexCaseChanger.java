/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.bst;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BibtexCaseChanger {

    private static final Log LOGGER = LogFactory.getLog(BibtexCaseChanger.class);

    // stores whether the char before the current char was a colon
    private boolean prevColon = true;

    // global variable to store the current brace level
    private int braceLevel;

    public enum FORMAT_MODE {
        // First character and character after a ":" as upper case - everything else in lower case. Obey {}.
        TITLE_LOWERS('t'),

        // All characters lower case - Obey {}
        ALL_LOWERS('l'),

        // all characters upper case - Obey {}
        ALL_UPPERS('u');

        // the following would have to be done if the functionality of CaseChangers would be included here
        // However, we decided against it and will probably do the other way round: https://github.com/JabRef/jabref/pull/215#issuecomment-146981624

        // Each word should start with a capital letter
        //EACH_FIRST_UPPERS('f'),

        // Converts all words to upper case, but converts articles, prepositions, and conjunctions to lower case
        // Capitalizes first and last word
        // Does not change words starting with "{"
        // DIFFERENCE to old CaseChangers.TITLE: last word is NOT capitalized in all cases
        //TITLE_UPPERS('T');

        public char asChar() {
            return asChar;
        }

        private final char asChar;

        private FORMAT_MODE(char asChar) {
            this.asChar = asChar;
        }

        /**
         * Convert bstFormat char into ENUM
         *
         * @throws IllegalArgumentException if char is not 't', 'l', 'u'
         */
        public static FORMAT_MODE getFormatModeForBSTFormat(final char bstFormat) {
            for (FORMAT_MODE mode : FORMAT_MODE.values()) {
                if (mode.asChar == bstFormat) {
                    return mode;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private BibtexCaseChanger() {
    }

    /**
     * Changes case of the given string s
     *
     * @param s the string to handle
     * @param format the format
     * @return
     */
    public static String changeCase(String s, FORMAT_MODE format) {
        return (new BibtexCaseChanger()).doChangeCase(s, format);
    }

    private String doChangeCase(String s, FORMAT_MODE format) {
        char[] c = s.toCharArray();

        StringBuffer sb = new StringBuffer();

        int i = 0;
        int n = s.length();

        while (i < n) {
            if (c[i] == '{') {
                braceLevel++;
                if ((braceLevel != 1) || ((i + 4) > n) || (c[i + 1] != '\\')) {
                    prevColon = false;
                    sb.append(c[i]);
                    i++;
                    continue;
                }
                if ((format == FORMAT_MODE.TITLE_LOWERS) && ((i == 0) || (prevColon && Character.isWhitespace(c[i - 1])))) {
                    assert(c[i] == '{');
                    sb.append('{');
                    i++;
                    prevColon = false;
                    continue;
                }
                i = convertSpecialChar(sb, c, i, format);
                continue;
            }
            if (c[i] == '}') {
                sb.append(c[i]);
                i++;
                if (braceLevel == 0) {
                    LOGGER.warn("Too many closing braces in string: " + s);
                } else {
                    braceLevel--;
                }
                prevColon = false;
                continue;
            }
            if (braceLevel == 0) {
                i = convertCharIfBraceLevelIsZero(c, i, sb, format);
                continue;
            }
            sb.append(c[i]);
            i++;
        }
        if (braceLevel > 0) {
            LOGGER.warn("No enough closing braces in string: " + s);
        }
        return sb.toString();
    }

    /**
     * We're dealing with a special character (usually either an undotted `\i'
     * or `\j', or an accent like one in Table~3.1 of the \LaTeX\ manual, or a
     * foreign character like one in Table~3.2) if the first character after the
     * |left_brace| is a |backslash|; the special character ends with the
     * matching |right_brace|. How we handle what's in between depends on the
     * special character. In general, this code will do reasonably well if there
     * is other stuff, too, between braces, but it doesn't try to do anything
     * special with |colon|s.
     *
     * @param c
     * @param i the current position. It points to the opening brace
     * @param format
     * @return
     */
    private int convertSpecialChar(StringBuffer sb, char[] c, int i, FORMAT_MODE format) {
        assert(c[i] == '{');

        sb.append(c[i]);
        i++; // skip over open brace

        while ((i < c.length) && (braceLevel > 0)) {
            sb.append(c[i]);
            i++;
            // skip over the |backslash|

            String s = BibtexCaseChanger.findSpecialChar(c, i);
            if (s != null) {
                i = convertAccented(c, i, s, sb, format);
            }

            while ((i < c.length) && (braceLevel > 0) && (c[i] != '\\')) {
                if (c[i] == '}') {
                    braceLevel--;
                } else if (c[i] == '{') {
                    braceLevel++;
                }
                i = convertNonControl(c, i, sb, format);
            }
        }
        return i;
    }

    /**
     * Convert the given string according to the format character (title, lower,
     * up) and append the result to the stringBuffer, return the updated
     * position.
     *
     * @param c
     * @param pos
     * @param s
     * @param sb
     * @param format
     * @return the new position
     */
    private int convertAccented(char[] c, int pos, String s, StringBuffer sb, FORMAT_MODE format) {
        pos += s.length();

        switch (format) {
        case TITLE_LOWERS:
        case ALL_LOWERS:
            if ("L O OE AE AA".contains(s)) {
                sb.append(s.toLowerCase());
            } else {
                sb.append(s);
            }
            break;
        case ALL_UPPERS:
            if ("l o oe ae aa".contains(s)) {
                sb.append(s.toUpperCase());
            } else if ("i j ss".contains(s)) {
                sb.deleteCharAt(sb.length() - 1); // Kill backslash
                sb.append(s.toUpperCase());
                while ((pos < c.length) && Character.isWhitespace(c[pos])) {
                    pos++;
                }
            } else {
                sb.append(s);
            }
            break;
        }
        return pos;
    }

    private int convertNonControl(char[] c, int pos, StringBuffer sb, FORMAT_MODE format) {
        switch (format) {
        case TITLE_LOWERS:
        case ALL_LOWERS:
            sb.append(Character.toLowerCase(c[pos]));
            pos++;
            break;
        case ALL_UPPERS:
            sb.append(Character.toUpperCase(c[pos]));
            pos++;
            break;
        }
        return pos;
    }

    private int convertCharIfBraceLevelIsZero(char[] c, int i, StringBuffer sb, FORMAT_MODE format) {
        switch (format) {
        case TITLE_LOWERS:
            if (i == 0) {
                sb.append(c[i]);
            } else if (prevColon && Character.isWhitespace(c[i - 1])) {
                sb.append(c[i]);
            } else {
                sb.append(Character.toLowerCase(c[i]));
            }
            if (c[i] == ':') {
                prevColon = true;
            } else if (!Character.isWhitespace(c[i])) {
                prevColon = false;
            }
            break;
        case ALL_LOWERS:
            sb.append(Character.toLowerCase(c[i]));
            break;
        case ALL_UPPERS:
            sb.append(Character.toUpperCase(c[i]));
        }
        i++;
        return i;
    }

    /**
     * Determine whether there starts a special char at pos (e.g., oe, AE). Return it as string.
     * If nothing found, return null
     *
     * Also used by BibtexPurify
     *
     * @param c the current "String"
     * @param pos the position
     * @return the special LaTeX character or null
     */
    static String findSpecialChar(char[] c, int pos) {
        if ((pos + 1) < c.length) {
            if ((c[pos] == 'o') && (c[pos + 1] == 'e')) {
                return "oe";
            }
            if ((c[pos] == 'O') && (c[pos + 1] == 'E')) {
                return "OE";
            }
            if ((c[pos] == 'a') && (c[pos + 1] == 'e')) {
                return "ae";
            }
            if ((c[pos] == 'A') && (c[pos + 1] == 'E')) {
                return "AE";
            }
            if ((c[pos] == 's') && (c[pos + 1] == 's')) {
                return "ss";
            }
            if ((c[pos] == 'A') && (c[pos + 1] == 'A')) {
                return "AA";
            }
            if ((c[pos] == 'a') && (c[pos + 1] == 'a')) {
                return "aa";
            }
        }
        if (c[pos] == 'i') {
            return String.valueOf(c[pos]);
        }
        if (c[pos] == 'j') {
            return String.valueOf(c[pos]);
        }
        if (c[pos] == 'o') {
            return String.valueOf(c[pos]);
        }
        if (c[pos] == 'O') {
            return String.valueOf(c[pos]);
        }
        if (c[pos] == 'l') {
            return String.valueOf(c[pos]);
        }
        if (c[pos] == 'L') {
            return String.valueOf(c[pos]);
        }
        return null;
    }
}
