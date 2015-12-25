package net.sf.jabref.logic.search;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchQueryHighlightObservableTest {

    @Test
    public void testUsage() {
        SearchQueryHighlightObservable observable = new SearchQueryHighlightObservable();
        assertEquals(0, observable.getListenerCount());
        observable.fireSearchlistenerEvent(new SearchQuery("test", false, false));
        SearchQueryHighlightListener listener = highlightPattern -> assertTrue("pattern must be there", highlightPattern.isPresent());
        observable.addSearchListener(listener);
        assertEquals(1, observable.getListenerCount());

        observable.addSearchListener(listener);
        assertEquals(1, observable.getListenerCount());

        observable.fireSearchlistenerEvent(new SearchQuery("test", true, true));
        observable.removeSearchListener(listener);
        assertEquals(0, observable.getListenerCount());

        observable.fireSearchlistenerEvent(new SearchQuery("author=harrer", true, true));
        observable.addSearchListener(p -> assertEquals(Optional.empty(), p));
        assertEquals(1, observable.getListenerCount());
    }

}