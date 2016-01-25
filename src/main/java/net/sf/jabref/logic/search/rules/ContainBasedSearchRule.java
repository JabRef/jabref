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
package net.sf.jabref.logic.search.rules;

import java.util.List;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.exporter.layout.format.RemoveLatexCommands;
import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.logic.search.rules.util.SentenceAnalyzer;

/**
 * Search rule for contain-based search.
 */
public class ContainBasedSearchRule implements SearchRule {

    private static final RemoveLatexCommands REMOVE_LATEX_COMMANDS = new RemoveLatexCommands();

    private final boolean caseSensitive;

    public ContainBasedSearchRule(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {

        String searchString = query;
        if (!caseSensitive) {
            searchString = searchString.toLowerCase();
        }

        List<String> words = new SentenceAnalyzer(searchString).getWords();

        // We need match for all words:
        boolean[] matchFound = new boolean[words.size()];

        for (String field : bibEntry.getFieldNames()) {
            if (bibEntry.hasField(field)) {
                String fieldContent = ContainBasedSearchRule.REMOVE_LATEX_COMMANDS.format(bibEntry.getField(field));
                if (!caseSensitive) {
                    fieldContent = fieldContent.toLowerCase();
                }

                int index = 0;
                // Check if we have a match for each of the query words, ignoring
                // those words for which we already have a match:
                for (String word : words) {
                    matchFound[index] = matchFound[index] || fieldContent.contains(word);

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

}
