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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Delivers possible completions for a given string.
 *
 * @author kahlert, cordes, olly98
 * @see AutoCompleterFactory
 */
public abstract class AbstractAutoCompleter implements AutoCompleter<String> {

    private static final int SHORTEST_WORD_TO_ADD = 4;
    private final AutoCompletePreferences preferences;

    /**
     * Stores the strings as is.
     */
    private final TreeSet<String> indexCaseSensitive = new TreeSet<>();

    /**
     * Stores strings in lowercase.
     */
    private final TreeSet<String> indexCaseInsensitive = new TreeSet<>();

    /**
     * Stores for a lowercase string the possible expanded strings.
     */
    private final Map<String, TreeSet<String>> possibleStringsForSearchString = new HashMap<>();


    public AbstractAutoCompleter(AutoCompletePreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    /**
     * {@inheritDoc}
     * The completion is case sensitive if the string contains upper case letters.
     * Otherwise the completion is case insensitive.
     */
    @Override
    public List<String> complete(String toComplete) {
        if(toComplete == null) {
            return new ArrayList<>();
        }
        if (isTooShortToComplete(toComplete)) {
            return new ArrayList<>();
        }
        String lowerCase = toComplete.toLowerCase();

        if (lowerCase.equals(toComplete)) {
            // user typed in lower case word -> we do an case-insensitive search
            String ender = AbstractAutoCompleter.incrementLastCharacter(lowerCase);
            SortedSet<String> subset = indexCaseInsensitive.subSet(lowerCase, ender);

            // As subset only contains lower case strings,
            // we have to to determine possible strings for each hit
            ArrayList<String> result = new ArrayList<>();
            for (String s : subset) {
                result.addAll(possibleStringsForSearchString.get(s));
            }
            return result;
        } else {
            // user typed in a mix of upper case and lower case,
            // we assume user wants to have exact search
            String ender = AbstractAutoCompleter.incrementLastCharacter(toComplete);
            SortedSet<String> subset = indexCaseSensitive.subSet(toComplete, ender);
            return new ArrayList<>(subset);
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

    /**
     * Returns whether the string is to short to be completed.
     */
    private boolean isTooShortToComplete(String toCheck) {
        return toCheck.length() < preferences.getShortestLengthToComplete();
    }

    @Override
    public void addItemToIndex(String word) {
        if (word.length() < getLengthOfShortestWordToAdd()) {
            return;
        }

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

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    public String getAutoCompleteText(String item) {
        return item;
    }

    protected int getLengthOfShortestWordToAdd() {
        return AbstractAutoCompleter.SHORTEST_WORD_TO_ADD;
    }
}
