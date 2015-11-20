package net.sf.jabref.logic.search;

import net.sf.jabref.logic.search.rules.util.SentenceAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchObservable {

    private final ArrayList<SearchTextListener> listeners = new ArrayList<>();

    private List<String> words = Collections.emptyList();

    /**
     * Adds a SearchTextListener to the search bar. The added listener is immediately informed about the current search.
     * Subscribers will be notified about searches.
     *
     * @param l SearchTextListener to be added
     */
    public void addSearchListener(SearchTextListener l) {
        if (listeners.contains(l)) {
            return;
        } else {
            listeners.add(l);
        }

        // fire event for the new subscriber
        l.searchText(words);
    }

    /**
     * Remove a SearchTextListener
     *
     * @param l SearchTextListener to be removed
     */
    public void removeSearchListener(SearchTextListener l) {
        listeners.remove(l);
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
     * @param searchText the search query
     */
    public void fireSearchlistenerEvent(SearchQuery searchQuery) {
        // Parse the search string to words
        if(searchQuery == null || !searchQuery.isContainsBasedSearch()) {
            words = Collections.emptyList();
        } else {
            words = getSearchwords(searchQuery.query);
        }

        update();
    }

    private void update() {
        // Fire an event for every listener
        for (SearchTextListener s : listeners) {
            s.searchText(words);
        }
    }

}
