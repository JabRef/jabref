/*  Copyright (C) 2003-2012 JabRef contributors.
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
package net.sf.jabref.logic.autocompleter;

import java.util.*;

import net.sf.jabref.model.entry.Author;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Delivers possible completions for a given string.
 * Interprets the given values as names and stores them in different
 * permutations so we can complete by beginning with last name or first name.
 */
class NameFieldAutoCompleter extends AbstractAutoCompleter {
    private static final int SHORTEST_WORD_TO_ADD = 1;

    private final List<String> fieldNames;
    /**
     * true if only last names should be completed and there is NO separation by " and ", but by " "
     */
    private final boolean lastNameOnlyAndSeparationBySpace;
    private final boolean autoCompFF;
    private final boolean autoCompLF;
    private final AutoCompleteFirstNameMode autoCompFirstnameMode;

    /**
     * @see AutoCompleterFactory
     */
    NameFieldAutoCompleter(String fieldName, AutoCompletePreferences preferences) {
        this(Collections.singletonList(Objects.requireNonNull(fieldName)), false, preferences);
    }

    public NameFieldAutoCompleter(List<String> fieldNames, boolean lastNameOnlyAndSeparationBySpace,
            AutoCompletePreferences preferences) {
        super(preferences);

        this.fieldNames = Objects.requireNonNull(fieldNames);
        this.lastNameOnlyAndSeparationBySpace = lastNameOnlyAndSeparationBySpace;
        if (preferences.getOnlyCompleteFirstLast()) {
            autoCompFF = true;
            autoCompLF = false;
        } else if (preferences.getOnlyCompleteLastFirst()) {
            autoCompFF = false;
            autoCompLF = true;
        } else {
            autoCompFF = true;
            autoCompLF = true;
        }
        autoCompFirstnameMode = preferences.getFirstnameMode() == null ? AutoCompleteFirstNameMode.BOTH : preferences
                .getFirstnameMode();
    }

    @Override
    public boolean isSingleUnitField() {
        // quick hack
        // when used at entry fields (!this.lastNameOnlyAndSeparationBySpace), this is a single unit field
        // when used at the search form (this.lastNameOnlyAndSeparationBySpace), this is NOT a single unit field
        // reason: search keywords are separated by space.
        //    This is OK for last names without prefix. "Lastname" works perfectly.
        //    querying for "van der Lastname" can be interpreted as
        //      a) "van" "der" "Lastname"
        //      b) "van der Lastname" (autocompletion lastname)
        return !this.lastNameOnlyAndSeparationBySpace;
    }

    @Override
    public void addToIndex(BibEntry entry) {
        if (entry == null) {
            return;
        }

        for (String fieldName : fieldNames) {
            Optional<String> fieldValue = entry.getFieldOptional(fieldName);

            if (fieldValue.isPresent()) {
                AuthorList authorList = AuthorList.parse(fieldValue.get());
                for (Author author : authorList.getAuthors()) {
                    handleAuthor(author);
                }
            }
        }
    }

    /**
     * Delimiter: " and " or " "
     *
     * @return String without prefix
     */
    private String getRemainder(String str, String delimiter) {
        String result = str;
        int index = result.toLowerCase().lastIndexOf(delimiter);

        if (index >= 0) {
            result = result.substring(index + delimiter.length());
        }
        return result;
    }

    private void handleAuthor(Author author) {
        if (lastNameOnlyAndSeparationBySpace) {
            insertIntoIndex(author.getLastOnly());
        } else {
            if (autoCompLF) {
                switch (autoCompFirstnameMode) {
                case ONLY_ABBREVIATED:
                    insertIntoIndex(author.getLastFirst(true));
                    break;
                case ONLY_FULL:
                    insertIntoIndex(author.getLastFirst(false));
                    break;
                case BOTH:
                    insertIntoIndex(author.getLastFirst(true));
                    insertIntoIndex(author.getLastFirst(false));
                    break;
                default:
                    break;
                }
            }
            if (autoCompFF) {
                switch (autoCompFirstnameMode) {
                case ONLY_ABBREVIATED:
                    insertIntoIndex(author.getFirstLast(true));
                    break;
                case ONLY_FULL:
                    insertIntoIndex(author.getFirstLast(false));
                    break;
                case BOTH:
                    insertIntoIndex(author.getFirstLast(true));
                    insertIntoIndex(author.getFirstLast(false));
                    break;
                default:
                    break;
                }
            }
        }

    }

    @Override
    public List<String> complete(String toComplete) {
        if (toComplete == null) {
            return Collections.emptyList();
        }

        String result;
        // Normally, one would implement that using
        // class inheritance. But this seemed overengineered
        if (this.lastNameOnlyAndSeparationBySpace) {
            result = getRemainder(toComplete, " ");
        } else {
            result = getRemainder(toComplete, " and ");
        }
        return super.complete(result);
    }

    @Override
    protected int getLengthOfShortestWordToAdd() {
        return SHORTEST_WORD_TO_ADD;
    }
}
