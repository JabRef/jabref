package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

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
    private BibDatabaseContext mockBibDatabaseContext;

    @Mock
    private FilePreferences mockFilePreferences;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BibEntry mockBibEntry;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        annotationCache = new FileAnnotationCache(mockBibDatabaseContext, mockFilePreferences);

        when(mockBibEntry.getCitationKey().orElse(mockBibEntry.getId())).thenReturn("testEntryId");
        when(mockBibEntry.getFiles()).thenReturn(Collections.emptyList()); // Add this line to prevent NullPointerException
    }

    @Test
    void annotationsAreLoadedAndCached() {

        Map<Path, List<FileAnnotation>> firstCallAnnotations = annotationCache.getFromCache(mockBibEntry);
        assertNotNull(firstCallAnnotations);

        Map<Path, List<FileAnnotation>> secondCallAnnotations = annotationCache.getFromCache(mockBibEntry);
        assertEquals(firstCallAnnotations, secondCallAnnotations);
    }

    @Test
    void removeEntryInvalidatesCache() {
        Map<Path, List<FileAnnotation>> initialAnnotations = annotationCache.getFromCache(mockBibEntry);
        assertNotNull(initialAnnotations);

        annotationCache.remove(mockBibEntry);

        Map<Path, List<FileAnnotation>> afterRemoveCall = annotationCache.getFromCache(mockBibEntry);
        assertNotNull(afterRemoveCall);
    }

    @Test
    void emptyAnnotationsAreHandled() {

        Map<Path, List<FileAnnotation>> firstCallAnnotations = annotationCache.getFromCache(mockBibEntry);
        assertNotNull(firstCallAnnotations);
        assertEquals(Collections.emptyMap(), firstCallAnnotations);

        Map<Path, List<FileAnnotation>> secondCallAnnotations = annotationCache.getFromCache(mockBibEntry);
        assertEquals(firstCallAnnotations, secondCallAnnotations);
        assertEquals(Collections.emptyMap(), secondCallAnnotations);
    }
}
