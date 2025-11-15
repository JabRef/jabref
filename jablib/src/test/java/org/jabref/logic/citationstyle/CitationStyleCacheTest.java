package org.jabref.logic.citationstyle;

import java.lang.reflect.Field;
import java.util.List;

import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitationStyleCacheTest {

    private BibEntry bibEntry;
    private List<BibEntry> entries;
    private BibDatabase database;
    private BibDatabaseContext databaseContext;
    private CitationStyleCache csCache;

    @Test
    void getCitationForTest() {
        BibEntry bibEntry = new BibEntry().withCitationKey("test");
        List<BibEntry> entries = List.of(bibEntry);
        BibDatabase database = new BibDatabase(entries);
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        CitationStyleCache csCache = new CitationStyleCache(databaseContext);

        assertNotNull(csCache.getCitationFor(bibEntry));
    }

    @Test
    void loadsFromCacheOnSecondAccess() throws Exception {
        BibEntry bibEntry = new BibEntry().withCitationKey("cacheKey");
        BibDatabase database = new BibDatabase(List.of(bibEntry));
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        CitationStyleCache cache = new CitationStyleCache(databaseContext);

        PreviewLayout previewLayout = mock(TextBasedPreviewLayout.class);
        when(previewLayout.generatePreview(bibEntry, databaseContext)).thenReturn("rendered-citation");

        // use reflection to set citationStyle field to mock
        Field field = CitationStyleCache.class.getDeclaredField("citationStyle");
        field.setAccessible(true);
        field.set(cache, previewLayout);

        String first = cache.getCitationFor(bibEntry);
        String second = cache.getCitationFor(bibEntry);

        assertEquals("rendered-citation", first);
        assertEquals("rendered-citation", second);
        verify(previewLayout, times(1)).generatePreview(bibEntry, databaseContext);
    }
}
