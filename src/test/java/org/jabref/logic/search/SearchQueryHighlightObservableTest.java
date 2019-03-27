package org.jabref.logic.search;

import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class SearchQueryHighlightObservableTest {

    @Captor ArgumentCaptor<Optional<Pattern>> captor;
    @Mock private SearchQueryHighlightListener listener;
    private SearchQueryHighlightObservable observable;

    @BeforeEach
    void setUp() throws Exception {
        observable = new SearchQueryHighlightObservable();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void addSearchListenerNotifiesListenerAboutPreviousPattern() throws Exception {
        observable.fireSearchlistenerEvent(new SearchQuery("test", false, false));

        observable.addSearchListener(listener);

        verify(listener).highlightPattern(captor.capture());
        assertEquals("(\\Qtest\\E)", captor.getValue().get().pattern());
    }

    @Test
    void addSearchListenerNotifiesRegisteredListener() throws Exception {
        observable.addSearchListener(listener);

        observable.fireSearchlistenerEvent(new SearchQuery("test", false, false));

        verify(listener, atLeastOnce()).highlightPattern(captor.capture());
        assertEquals("(\\Qtest\\E)", captor.getValue().get().pattern());
    }

    @Test
    void addSearchListenerNotifiesRegisteredListenerAboutGrammarBasedSearches() throws Exception {
        observable.addSearchListener(listener);

        observable.fireSearchlistenerEvent(new SearchQuery("author=harrer", false, false));

        verify(listener, atLeastOnce()).highlightPattern(captor.capture());
        // TODO: We would expect "harrer" here
        assertEquals("(\\Qauthor=harrer\\E)", captor.getValue().get().pattern());
    }
}
