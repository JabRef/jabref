/*  Copyright (C) 2003-2011 JabRef contributors.
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
// $Id$
package net.sf.jabref.bst;

/**
 *
 * The |built_in| function {\.{purify\$}} pops the top (string) literal, removes
 * nonalphanumeric characters except for |white_space| and |sep_char| characters
 * (these get converted to a |space|) and removes certain alphabetic characters
 * contained in the control sequences associated with a special character, and
 * pushes the resulting string. If the literal isn't a string, it complains and
 * pushes the null string.
 *
 */
public class BibtexPurify {

    /**
     *
     * @param toPurify
     * @param warn
     *            may-be-null
     * @return
     */
    public static String purify(String toPurify, Warn warn) {

        StringBuilder sb = new StringBuilder();

        char[] cs = toPurify.toCharArray();
        int n = cs.length;
        int i = 0;

        int braceLevel = 0;

        while (i < n) {
            char c = cs[i];
            if (Character.isWhitespace(c) || (c == '-') || (c == '~')) {
                sb.append(' ');
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (c == '{') {
                braceLevel++;
                if ((braceLevel == 1) && ((i + 1) < n) && (cs[i + 1] == '\\')) {
                    i++; // skip brace
                    while ((i < n) && (braceLevel > 0)) {
                        i++; // skip backslash
                        BibtexCaseChanger.findSpecialChar(cs, i).ifPresent(sb::append);

                        while ((i < n) && Character.isLetter(cs[i])) {
                            i++;
                        }
                        while ((i < n) && (braceLevel > 0) && ((c = cs[i]) != '\\')) {
                            if (Character.isLetterOrDigit(c)) {
                                sb.append(c);
                            } else if (c == '}') {
                                braceLevel--;
                            } else if (c == '{') {
                                braceLevel++;
                            }
                            i++;
                        }
                    }
                    continue;
                }
            } else if (c == '}') {
                if (braceLevel > 0) {
                    braceLevel--;
                } else {
                    if (warn != null) {
                        warn.warn("Unbalanced brace in string for purify$: " + toPurify);
                    }
                }
            }
            i++;
        }
        if ((braceLevel != 0) && (warn != null)) {
            warn.warn("Unbalanced brace in string for purify$: " + toPurify);
        }

        return sb.toString();
    }
}
