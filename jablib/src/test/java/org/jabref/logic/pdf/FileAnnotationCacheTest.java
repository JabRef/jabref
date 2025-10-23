package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class FileAnnotationCacheTest {

    private FileAnnotationCache annotationCache;

    @Mock
    private BibDatabaseContext bibDatabaseContext;

    @Mock
    private FilePreferences filePreferences;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BibEntry bibEntry;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        annotationCache = new FileAnnotationCache(bibDatabaseContext, filePreferences);

        when(bibEntry.getCitationKey().orElse(bibEntry.getId())).thenReturn("testEntryId");
        when(bibEntry.getFiles()).thenReturn(Collections.emptyList()); // Add this line to prevent NullPointerException
    }

    @Test
    void annotationsAreLoadedAndCached() {
        Map<Path, List<FileAnnotation>> firstCallAnnotations = annotationCache.getFromCache(bibEntry);
        assertNotNull(firstCallAnnotations);

        Map<Path, List<FileAnnotation>> secondCallAnnotations = annotationCache.getFromCache(bibEntry);
        assertEquals(firstCallAnnotations, secondCallAnnotations);
    }

    @Test
    void removeEntryInvalidatesCache() {
        Map<Path, List<FileAnnotation>> initialAnnotations = annotationCache.getFromCache(bibEntry);
        assertNotNull(initialAnnotations);

        annotationCache.remove(bibEntry);

        Map<Path, List<FileAnnotation>> afterRemoveCall = annotationCache.getFromCache(bibEntry);
        assertNotNull(afterRemoveCall);
    }

    @Test
    void emptyAnnotationsAreHandled() {
        Map<Path, List<FileAnnotation>> firstCallAnnotations = annotationCache.getFromCache(bibEntry);
        assertNotNull(firstCallAnnotations);
        assertEquals(Collections.emptyMap(), firstCallAnnotations);

        Map<Path, List<FileAnnotation>> secondCallAnnotations = annotationCache.getFromCache(bibEntry);
        assertEquals(firstCallAnnotations, secondCallAnnotations);
        assertEquals(Collections.emptyMap(), secondCallAnnotations);
    }
}
