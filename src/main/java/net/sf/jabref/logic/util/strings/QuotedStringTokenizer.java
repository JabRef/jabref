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
package net.sf.jabref.logic.util.strings;

/**
 * A String tokenizer that works just like StringTokenizer, but considers quoted
 * characters (which do not act as delimiters).
 */
public class QuotedStringTokenizer {

    private final String content;
    private final int contentLength;
    private final String delimiters;
    private final char quoteChar;
    private int index;


    /**
     * @param content
     *            The String to be tokenized.
     * @param delimiters
     *            The delimiter characters.
     * @param quoteCharacter
     *            The quoting character. Every character (including, but not
     *            limited to, delimiters) that is preceded by this character is
     *            not treated as a delimiter, but as a token component.
     */
    public QuotedStringTokenizer(String content, String delimiters, char quoteCharacter) {
        this.content = content;
        this.delimiters = delimiters;
        quoteChar = quoteCharacter;
        contentLength = this.content.length();
        // skip leading delimiters
        while (isDelimiter(this.content.charAt(index)) && index < contentLength) {
            ++index;
        }
    }

    public String nextToken() {
        char c;
        StringBuilder stringBuilder = new StringBuilder();
        while (index < contentLength) {
            c = content.charAt(index);
            if (c == quoteChar) { // next is quoted
                ++index;
                if (index < contentLength) {
                    stringBuilder.append(content.charAt(index));
                    // ignore for delimiter search!
                }
            } else if (isDelimiter(c)) { // unit finished
                // advance index until next token or end
                do {
                    ++index;
                } while (index < contentLength && isDelimiter(content.charAt(index)));
                return stringBuilder.toString();
            } else {
                stringBuilder.append(c);
            }
            ++index;
        }
        return stringBuilder.toString();
    }

    private boolean isDelimiter(char c) {
        return delimiters.indexOf(c) >= 0;
    }

    public boolean hasMoreTokens() {
        return index < contentLength;
    }
}
