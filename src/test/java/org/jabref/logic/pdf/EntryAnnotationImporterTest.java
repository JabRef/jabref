package org.jabref.logic.pdf;


import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.pdf.FileAnnotation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntryAnnotationImporterTest {

    private BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private BibEntry entry = new BibEntry("EntryKey");

    @Before
    public void setUp() {
        when(databaseContext.getFileDirectoriesAsPaths(any())).thenReturn(Collections.singletonList(Paths.get("src/test/resources/pdfs/")));
    }

    @Test
    public void readEntryExampleThesis() {
        //given
        entry.setField(FieldName.FILE, ":thesis-example.pdf:PDF");
        EntryAnnotationImporter entryAnnotationImporter = new EntryAnnotationImporter(entry);

        //when
        Map<String, List<FileAnnotation>> annotations = entryAnnotationImporter.importAnnotationsFromFiles(databaseContext);

        //then
        int fileCounter = 0;
        int annotationCounter = 0;
        for (List<FileAnnotation> annotationsOfFile : annotations.values()) {
            fileCounter++;
            annotationCounter += annotationsOfFile.size();
        }
        assertEquals(1, fileCounter);
        assertEquals(2, annotationCounter);
    }
}
