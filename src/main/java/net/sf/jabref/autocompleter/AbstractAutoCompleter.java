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
package net.sf.jabref.autocompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.jabref.BibtexEntry;

/**
 * An autocompleter delivers possible completions for a given String. There are
 * different types of autocompleters for different use cases.
 * 
 * Example: {@link NameFieldAutoCompleter}, {@link EntireFieldAutoCompleter}
 * 
 * @author kahlert, cordes, olly98
 * @see AutoCompleterFactory
 */
abstract class AbstractAutoCompleter implements AutoCompleter {

    private static final int SHORTEST_WORD = 4;

    // stores the strings as is
    private final TreeSet<String> indexCaseSensitive = new TreeSet<String>();

    // stores strings in lowercase
    private final TreeSet<String> indexCaseInsensitive = new TreeSet<String>();

    // stores for a lowercase string the possible expanded strings
    private final HashMap<String, TreeSet<String>> possibleStringsForSearchString = new HashMap<String, TreeSet<String>>();



    abstract public void addBibtexEntry(BibtexEntry entry);

    /**
     * Returns one or more possible completions for a given String. The returned
     * completion depends on which informations were stored while adding
     * BibtexEntries by the used implementation of {@link AbstractAutoCompleter}
     * .
     * 
     * @see AbstractAutoCompleter#addBibtexEntry(BibtexEntry)
     */
    public String[] complete(String str) {
        if (AbstractAutoCompleter.stringMinLength(str)) {
            return null;
        }
        String lstr = str.toLowerCase();

        if (lstr.equals(str)) {
            // user typed in lower case word -> we do an case-insenstive search
            String ender = AbstractAutoCompleter.incrementLastCharacter(lstr);
            SortedSet<String> subset = indexCaseInsensitive.subSet(lstr, ender);

            // As subset only contains lower case strings, 
            // we have to to determine possible strings for each hit
            ArrayList<String> res = new ArrayList<String>();
            for (String s : subset) {
                res.addAll(possibleStringsForSearchString.get(s));
            }
            return res.toArray(new String[res.size()]);
        } else {
            // user typed in a mix of upper case and lower case,
            // we assume user wants to have exact search
            String ender = AbstractAutoCompleter.incrementLastCharacter(str);
            SortedSet<String> subset = indexCaseSensitive.subSet(str, ender);
            return subset.toArray(new String[subset.size()]);
        }
    }

    /**
     * Increments the last character of a string.
     * 
     * Example: incrementLastCharacter("abc") returns "abd".
     */
    private static String incrementLastCharacter(String str) {
        char lastChar = str.charAt(str.length() - 1);
        return str.substring(0, str.length() - 1) + Character.toString((char) (lastChar + 1));
    }

    private static boolean stringMinLength(String str) {
        return str.length() < AutoCompleterFactory.SHORTEST_TO_COMPLETE;
    }

    public void addWordToIndex(String word) {
        if (word.length() >= AbstractAutoCompleter.SHORTEST_WORD) {
            indexCaseSensitive.add(word);

            // insensitive treatment
            // first, add the lower cased word to search index
            // second, add a mapping from the lower cased word to the real word
            String word_lcase = word.toLowerCase();
            indexCaseInsensitive.add(word_lcase);
            TreeSet<String> set = possibleStringsForSearchString.get(word_lcase);
            if (set == null) {
                set = new TreeSet<String>();
            }
            set.add(word);
            possibleStringsForSearchString.put(word_lcase, set);
        }
    }

    public boolean indexContainsWord(String word) {
        return indexCaseInsensitive.contains(word.toLowerCase());
    }

    public String getPrefix() {
        return "";
    }

}
