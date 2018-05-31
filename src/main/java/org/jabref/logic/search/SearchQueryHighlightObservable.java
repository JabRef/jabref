package org.jabref.logic.search;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import com.google.common.eventbus.EventBus;

public class SearchQueryHighlightObservable {

    private final EventBus eventBus = new EventBus();

    private Optional<Pattern> pattern = Optional.empty();

    /**
     * Adds a SearchQueryHighlightListener to the search bar. The added listener is immediately informed about the current search.
     * Subscribers will be notified about searches.
     *
     * @param newListener SearchQueryHighlightListener to be added
     */
    public void addSearchListener(SearchQueryHighlightListener newListener) {
        Objects.requireNonNull(newListener);

        eventBus.register(newListener);
        newListener.highlightPattern(pattern);

    }

    public void removeSearchListener(SearchQueryHighlightListener listener) {
        Objects.requireNonNull(listener);

        try {
            eventBus.unregister(listener);
        } catch (IllegalArgumentException e) {
            // occurs if the event source has not been registered, should not prevent shutdown
        }
    }
    /**
     * Fires an event if a search was started (or cleared)
     *
     * @param searchQuery the search query
     */

    public void fireSearchlistenerEvent(SearchQuery searchQuery) {
        Objects.requireNonNull(searchQuery);

        // Parse the search string to words
        pattern = getPatternForWords(searchQuery.getSearchWords(), searchQuery.isRegularExpression(),
                searchQuery.isCaseSensitive());

        update();
    }

    public void reset() {
        pattern = Optional.empty();
        update();
    }

    private void update() {
        // Fire an event for every listener
        eventBus.post(pattern);
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
