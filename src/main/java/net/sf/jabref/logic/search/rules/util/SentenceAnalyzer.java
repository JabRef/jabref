/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.search.rules.util;

import java.util.ArrayList;
import java.util.List;

public class SentenceAnalyzer {

    public static final char ESCAPE_CHAR = '\\';
    public static final char QUOTE_CHAR = '"';

    private final String query;

    public SentenceAnalyzer(String query) {
        this.query = query;
    }

    public List<String> getWords() {
        List<String> result = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();
        boolean escaped = false;
        boolean quoted = false;
        for(char c : query.toCharArray()) {
            // Check if we are entering an escape sequence:
            if (!escaped && c == ESCAPE_CHAR) {
                escaped = true;
            } else {
                // See if we have reached the end of a word:
                if (!escaped && !quoted && Character.isWhitespace(c)) {
                    if (stringBuilder.length() > 0) {
                        result.add(stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                    }
                } else if (c == QUOTE_CHAR) {
                    // Whether it is a start or end quote, store the current
                    // word if any:
                    if (stringBuilder.length() > 0) {
                        result.add(stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                    }
                    quoted = !quoted;
                } else {
                    // All other possibilities exhausted, we add the char to
                    // the current word:
                    stringBuilder.append(c);
                }
                escaped = false;
            }
        }
        // Finished with the loop. If we have a current word, add it:
        if (stringBuilder.length() > 0) {
            result.add(stringBuilder.toString());
        }

        return result;
    }
}
