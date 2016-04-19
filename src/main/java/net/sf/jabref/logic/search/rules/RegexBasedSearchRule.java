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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jabref.logic.layout.format.RemoveLatexCommands;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Search rule for regex-based search.
 */
public class RegexBasedSearchRule implements SearchRule {

    private static final RemoveLatexCommands REMOVE_LATEX_COMMANDS = new RemoveLatexCommands();

    private final boolean caseSensitive;

    public RegexBasedSearchRule(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        String searchString = query;
        if (!caseSensitive) {
            searchString = searchString.toLowerCase();
        }

        try {
            Pattern.compile(searchString, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean applyRule(String query, BibEntry bibEntry) {
        Pattern pattern;

        try {
            pattern = Pattern.compile(query, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            return false;
        }

        for (String field : bibEntry.getFieldNames()) {
            if (bibEntry.hasField(field)) {
                String fieldContent = RegexBasedSearchRule.REMOVE_LATEX_COMMANDS.format(bibEntry.getField(field));
                String fieldContentNoBrackets = RegexBasedSearchRule.REMOVE_LATEX_COMMANDS.format(fieldContent);
                Matcher m = pattern.matcher(fieldContentNoBrackets);
                if (m.find()) {
                    return true;
                }
            }

        }
        return false;
    }

}
