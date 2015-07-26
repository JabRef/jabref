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
package net.sf.jabref.search.rules;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.export.layout.format.RemoveLatexCommands;
import net.sf.jabref.search.SearchRule;

/**
 * Search rule for simple search.
 */
public class BasicSearchRule implements SearchRule {

    private static final RemoveLatexCommands REMOVE_LATEX_COMMANDS = new RemoveLatexCommands();

    protected final boolean caseSensitive;

    public BasicSearchRule(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }

    @Override
    public boolean applyRule(String query, BibtexEntry bibtexEntry) {

        String searchString = query;
        if (!caseSensitive) {
            searchString = searchString.toLowerCase();
        }

        List<String> words = parseQuery(searchString);

        //print(words);
        // We need match for all words:
        boolean[] matchFound = new boolean[words.size()];

        //TODO build upon already existing SimpleSearchRule
        for (String field : bibtexEntry.getAllFields()) {
            Object fieldContentAsObject = bibtexEntry.getField(field);
            if (fieldContentAsObject != null) {
                String fieldContent = BasicSearchRule.REMOVE_LATEX_COMMANDS.format(fieldContentAsObject.toString());
                if (!caseSensitive) {
                    fieldContent = fieldContent.toLowerCase();
                }

                int index = 0;
                // Check if we have a match for each of the query words, ignoring
                // those words for which we already have a match:
                for (String s : words) {
                    matchFound[index] = matchFound[index] || fieldContent.contains(s);

                    index++;
                }
            }

        }
        for (boolean aMatchFound : matchFound) {
            if (!aMatchFound) {
                return false; // Didn't match all words.
            }
        }
        return true; // Matched all words.
    }

    public static List<String> parseQuery(String query) {
        return new SentenceAnalyzer(query).getWords();
    }

    public static class SentenceAnalyzer {

        public static final char ESCAPE_CHAR = '\\';
        public static final char QUOTE_CHAR = '"';

        private final String query;

        public SentenceAnalyzer(String query) {
            this.query = query;
        }

        public List<String> getWords() {
            List<String> result = new ArrayList<String>();

            StringBuffer sb = new StringBuffer();
            boolean escaped = false;
            boolean quoted = false;
            for(char c : query.toCharArray()) {
                // Check if we are entering an escape sequence:
                if (!escaped && (c == ESCAPE_CHAR)) {
                    escaped = true;
                } else {
                    // See if we have reached the end of a word:
                    if (!escaped && !quoted && Character.isWhitespace(c)) {
                        if (sb.length() > 0) {
                            result.add(sb.toString());
                            sb = new StringBuffer();
                        }
                    } else if (c == QUOTE_CHAR) {
                        // Whether it is a start or end quote, store the current
                        // word if any:
                        if (sb.length() > 0) {
                            result.add(sb.toString());
                            sb = new StringBuffer();
                        }
                        quoted = !quoted;
                    } else {
                        // All other possibilities exhausted, we add the char to
                        // the current word:
                        sb.append(c);
                    }
                    escaped = false;
                }
            }
            // Finished with the loop. If we have a current word, add it:
            if (sb.length() > 0) {
                result.add(sb.toString());
            }

            return result;
        }
    }
}
