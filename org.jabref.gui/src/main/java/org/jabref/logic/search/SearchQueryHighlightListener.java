package org.jabref.logic.search;

import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.eventbus.Subscribe;

/**
 * Every Listener that wants to receive events from a search needs to
 * implement this interface
 *
 * @author Ben
 *
 */
@FunctionalInterface
public interface SearchQueryHighlightListener {

    /**
     * Pattern with which one can determine what to highlight
     *
     * @param words null if nothing is searched for
     */
    @Subscribe
    void highlightPattern(Optional<Pattern> highlightPattern);
}
