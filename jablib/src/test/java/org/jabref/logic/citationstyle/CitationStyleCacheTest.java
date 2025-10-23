package org.jabref.logic.citationstyle;

import java.lang.reflect.Field;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class CitationStyleCacheTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BibEntry bibEntry;
    private List<BibEntry> entries;
    private BibDatabase database;

    @Mock
    private BibDatabaseContext databaseContext;
    private CitationStyleCache csCache;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        BibDatabase mockDatabase = new BibDatabase();
        when(databaseContext.getDatabase()).thenReturn(mockDatabase);
        csCache = new CitationStyleCache(databaseContext);
    }

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
    void testCitationStyleCacheType() throws NoSuchFieldException, IllegalAccessException {
        Field cacheField = CitationStyleCache.class.getDeclaredField("citationStyleCache");
        cacheField.setAccessible(true);
        Object cacheInstance = cacheField.get(csCache);

        assertNotNull(cacheInstance);
        assert (cacheInstance.getClass().getName().contains("com.github.benmanes.caffeine.cache"));
    }
}
