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
package net.sf.jabref.logic.autocompleter;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.jabref.model.entry.BibtexEntry;

/**
 * An autocompleter delivers possible completions for a given string. There are different types of autocompleters for
 * different use cases.
 * 
 * Example: {@link NameFieldAutoCompleter}, {@link EntireFieldAutoCompleter}
 *
 * @author kahlert, cordes, olly98
 * @see AutoCompleterFactory
 */
public abstract class AbstractAutoCompleter implements AutoCompleter<String> {

    private static final int SHORTEST_WORD = 4;

    // stores the strings as is
    private final TreeSet<String> indexCaseSensitive = new TreeSet<>();

    // stores strings in lowercase
    private final TreeSet<String> indexCaseInsensitive = new TreeSet<>();

    // stores for a lowercase string the possible expanded strings
    private final HashMap<String, TreeSet<String>> possibleStringsForSearchString = new HashMap<>();

    @Override
    public abstract void addBibtexEntry(BibtexEntry entry);

    /**
     * Returns one or more possible completions for a given String. The returned
     * completion depends on which informations were stored while adding
     * BibtexEntries by the used implementation of {@link AbstractAutoCompleter}
     * .
     *
     * @see AbstractAutoCompleter#addBibtexEntry(BibtexEntry)
     */
    @Override
    public String[] complete(String toComplete) {
        if (AbstractAutoCompleter.stringMinLength(toComplete)) {
            return null;
        }
        String lowerCase = toComplete.toLowerCase();

        if (lowerCase.equals(toComplete)) {
            // user typed in lower case word -> we do an case-insenstive search
            String ender = AbstractAutoCompleter.incrementLastCharacter(lowerCase);
            SortedSet<String> subset = indexCaseInsensitive.subSet(lowerCase, ender);

            // As subset only contains lower case strings,
            // we have to to determine possible strings for each hit
            ArrayList<String> result = new ArrayList<>();
            for (String s : subset) {
                result.addAll(possibleStringsForSearchString.get(s));
            }
            return result.toArray(new String[result.size()]);
        } else {
            // user typed in a mix of upper case and lower case,
            // we assume user wants to have exact search
            String ender = AbstractAutoCompleter.incrementLastCharacter(toComplete);
            SortedSet<String> subset = indexCaseSensitive.subSet(toComplete, ender);
            return subset.toArray(new String[subset.size()]);
        }
    }

    /**
     * Increments the last character of a string.
     *
     * Example: incrementLastCharacter("abc") returns "abd".
     */
    private static String incrementLastCharacter(String toIncrement) {
        if (toIncrement.isEmpty()) {
            return "";
        }

        char lastChar = toIncrement.charAt(toIncrement.length() - 1);
        return toIncrement.substring(0, toIncrement.length() - 1) + Character.toString((char) (lastChar + 1));
    }

    private static boolean stringMinLength(String toCheck) {
        return toCheck.length() < AutoCompleterFactory.SHORTEST_TO_COMPLETE;
    }

    @Override
    public void addWordToIndex(String word) {
        if (word.length() >= AbstractAutoCompleter.SHORTEST_WORD) {
            indexCaseSensitive.add(word);

            // insensitive treatment
            // first, add the lower cased word to search index
            // second, add a mapping from the lower cased word to the real word
            String lowerCase = word.toLowerCase();
            indexCaseInsensitive.add(lowerCase);
            TreeSet<String> set = possibleStringsForSearchString.get(lowerCase);
            if (set == null) {
                set = new TreeSet<>();
            }
            set.add(word);
            possibleStringsForSearchString.put(lowerCase, set);
        }
    }

    @Override
    public boolean indexContainsWord(String word) {
        return indexCaseInsensitive.contains(word.toLowerCase());
    }

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    public String getAutoCompleteText(String item) {
        return item.toString();
    }
}
