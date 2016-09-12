package net.sf.jabref.logic.autocompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;

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
    private final Map<String, Set<String>> possibleStringsForSearchString = new HashMap<>();


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
            List<String> result = new ArrayList<>();
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

        word = new LatexToUnicodeFormatter().format(word);

        indexCaseSensitive.add(word);

        // insensitive treatment
        // first, add the lower cased word to search index
        // second, add a mapping from the lower cased word to the real word
        String lowerCase = word.toLowerCase();
        indexCaseInsensitive.add(lowerCase);
        Set<String> set = possibleStringsForSearchString.get(lowerCase);
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
