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

import java.util.*;

/**
 * Delivers possible completions for a given string.
 *
 * @see AutoCompleterFactory
 */
public abstract class AbstractAutoCompleter implements AutoCompleter<String> {
    private static final int SHORTEST_WORD_TO_ADD = 4;

    private final AutoCompletePreferences preferences;
    private final TreeSet<String> caseSensitiveIndex = new TreeSet<>();

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
        if (toComplete == null || !hasMinimumLength(toComplete)) {
            return Collections.emptyList();
        }

        String nextWord = incrementLastCharacter(toComplete);
        SortedSet<String> matchingWords = caseSensitiveIndex.subSet(toComplete, nextWord);
        return new ArrayList<>(matchingWords);
    }

    /**
     * Increments the last character of a string.
     * <p>
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
    private boolean hasMinimumLength(String toCheck) {
        return toCheck.length() >= preferences.getMinLengthToComplete();
    }

    @Override
    public void insertIntoIndex(String word) {
        if (word.length() < getLengthOfShortestWordToAdd()) {
            return;
        }

        caseSensitiveIndex.add(word);
    }

    @Override
    public String getAutoCompleteText(String item) {
        return item;
    }

    /**
     * Returns the minumum length of words that should be added to the auto-completion index.
     *
     * @return the minimun length of words inside the index
     */
    protected int getLengthOfShortestWordToAdd() {
        return SHORTEST_WORD_TO_ADD;
    }
}
