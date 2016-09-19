package net.sf.jabref.logic.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import net.sf.jabref.model.search.rules.SentenceAnalyzer;

public class SearchQueryHighlightObservable {

    private final List<SearchQueryHighlightListener> listeners = new ArrayList<>();

    private Optional<Pattern> pattern = Optional.empty();

    /**
     * Adds a SearchQueryHighlightListener to the search bar. The added listener is immediately informed about the current search.
     * Subscribers will be notified about searches.
     *
     * @param newListener SearchQueryHighlightListener to be added
     */
    public SearchQueryHighlightObservable addSearchListener(SearchQueryHighlightListener newListener) {
        Objects.requireNonNull(newListener);

        if (!listeners.contains(newListener)) {
            listeners.add(newListener);
            newListener.highlightPattern(pattern);
        }

        return this;
    }

    /**
     * Parses the search query for valid words and returns a list these words. For example, "The great Vikinger" will
     * give ["The","great","Vikinger"]
     *
     * @param searchText the search query
     * @return list of words found in the search query
     */
    private List<String> getSearchwords(String searchText) {
        return (new SentenceAnalyzer(searchText)).getWords();
    }

    /**
     * Fires an event if a search was started (or cleared)
     *
     * @param searchQuery the search query
     */
    public void fireSearchlistenerEvent(SearchQuery searchQuery) {
        Objects.requireNonNull(searchQuery);

        // Parse the search string to words
        if (searchQuery.isGrammarBasedSearch()) {
            pattern = Optional.empty();
        } else if (searchQuery.isRegularExpression()) {
            pattern = getPatternForWords(Collections.singletonList(searchQuery.getQuery()), true, searchQuery.isCaseSensitive());
        } else {
            pattern = getPatternForWords(getSearchwords(searchQuery.getQuery()), searchQuery.isRegularExpression(), searchQuery.isCaseSensitive());
        }

        update();
    }

    public void reset() {
        pattern = Optional.empty();
        update();
    }

    private void update() {
        // Fire an event for every listener
        for (SearchQueryHighlightListener s : listeners) {
            s.highlightPattern(pattern);
        }
    }

    // Returns a regular expression pattern in the form (w1)|(w2)| ... wi are escaped if no regular expression search is enabled
    public static Optional<Pattern> getPatternForWords(List<String> words, boolean useRegex, boolean isCaseSensitive) {
        if ((words == null) || words.isEmpty() || words.get(0).isEmpty()) {
            return Optional.empty();
        }

        // compile the words to a regular expression in the form (w1)|(w2)|(w3)
        StringJoiner joiner = new StringJoiner(")|(", "(", ")");
        for (String word : words) {
            joiner.add(useRegex ? word : Pattern.quote(word));
        }
        String searchPattern = joiner.toString();

        if (isCaseSensitive) {
            return Optional.of(Pattern.compile(searchPattern));
        } else {
            return Optional.of(Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE));
        }
    }

}
