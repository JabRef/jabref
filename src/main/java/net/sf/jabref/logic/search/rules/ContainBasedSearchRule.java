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

import java.util.Iterator;
import java.util.List;

import net.sf.jabref.logic.layout.format.RemoveLatexCommands;
import net.sf.jabref.model.entry.BibEntry;

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

        List<String> unmatchedWords = new SentenceAnalyzer(searchString).getWords();

        for (String fieldContent : bibEntry.getFieldValues()) {
            String formattedFieldContent = ContainBasedSearchRule.REMOVE_LATEX_COMMANDS.format(fieldContent);
            if (!caseSensitive) {
                formattedFieldContent = formattedFieldContent.toLowerCase();
            }

            Iterator<String> unmatchedWordsIterator = unmatchedWords.iterator();
            while (unmatchedWordsIterator.hasNext()) {
                String word = unmatchedWordsIterator.next();
                if(formattedFieldContent.contains(word)) {
                    unmatchedWordsIterator.remove();
                }
            }

            if(unmatchedWords.isEmpty()) {
                return true;
            }
        }

        return false; // Didn't match all words.
    }

}
