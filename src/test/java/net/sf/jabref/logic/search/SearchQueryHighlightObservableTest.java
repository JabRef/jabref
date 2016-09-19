package net.sf.jabref.logic.search;

import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SearchQueryHighlightObservableTest {

        private SearchQueryHighlightObservable observable;
        private SearchQueryHighlightListener listener;
    @Captor ArgumentCaptor<Optional<Pattern>> captor;

    @Before
    public void setUp() throws Exception {
        observable = new SearchQueryHighlightObservable();
        listener = mock(SearchQueryHighlightListener.class);
    }

    @Test
    public void addSearchListenerNotifiesListenerAboutPreviousPattern() throws Exception {
        observable.fireSearchlistenerEvent(new SearchQuery("test", false, false));

        observable.addSearchListener(listener);

        verify(listener).highlightPattern(captor.capture());
        assertEquals("(\\Qtest\\E)", captor.getValue().get().pattern());
    }

    @Test
    public void addSearchListenerNotifiesRegisteredListener() throws Exception {
        observable.addSearchListener(listener);

        observable.fireSearchlistenerEvent(new SearchQuery("test", false, false));

        verify(listener, atLeastOnce()).highlightPattern(captor.capture());
        assertEquals("(\\Qtest\\E)", captor.getValue().get().pattern());
    }

    @Test
    public void addSearchListenerDoesNotNotifiesRegisteredListenerAboutGrammarBasedSearches() throws Exception {
        observable.addSearchListener(listener);

        observable.fireSearchlistenerEvent(new SearchQuery("author=harrer", false, false));

        verify(listener, atLeastOnce()).highlightPattern(captor.capture());
        assertEquals(Optional.empty(), captor.getValue());
    }
}
