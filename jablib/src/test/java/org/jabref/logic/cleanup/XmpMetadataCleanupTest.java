package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.jabref.logic.xmp.DublinCoreExtractor.DC_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmpMetadataCleanupTest {

    @TempDir
    Path tempDir;

    private BibDatabaseContext databaseContext;
    private XmpMetadataCleanup cleanupJob;
    private XmpUtilReader xmpReader;
    private XmpUtilWriter xmpWriter;

    @BeforeEach
    void setUp() {
        databaseContext = mock(BibDatabaseContext.class);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        cleanupJob = new XmpMetadataCleanup(databaseContext, filePreferences);
        xmpWriter = new XmpUtilWriter(xmpPreferences);
        xmpReader = new XmpUtilReader();
    }

    @Test
    void cleanupRemovesXmpMetadata() throws IOException, TransformerException {
        Path pdfFile = tempDir.resolve("olly2018.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfFile.toFile());
        }
        BibEntry olly2018 = new BibEntry(StandardEntryType.Article);
        olly2018.setCitationKey("Olly2018");
        olly2018.setField(StandardField.AUTHOR, "Olly and Johannes");
        olly2018.setField(StandardField.TITLE, "Stefan's palace");
        olly2018.setField(StandardField.JOURNAL, "Test Journal");
        olly2018.setField(StandardField.VOLUME, "1");
        olly2018.setField(StandardField.NUMBER, "1");
        olly2018.setField(StandardField.PAGES, "1-2");
        olly2018.setMonth(Month.MARCH);
        olly2018.setField(StandardField.ISSN, "978-123-123");
        olly2018.setField(StandardField.NOTE, "NOTE");
        olly2018.setField(StandardField.ABSTRACT, "ABSTRACT");
        olly2018.setField(StandardField.COMMENT, "COMMENT");
        olly2018.setField(StandardField.DOI, "10/3212.3123");
        olly2018.setField(StandardField.FILE, ":article_dublinCore.pdf:PDF");
        olly2018.setField(StandardField.GROUPS, "NO");
        olly2018.setField(StandardField.HOWPUBLISHED, "online");
        olly2018.setField(StandardField.KEYWORDS, "k1, k2");
        olly2018.setField(StandardField.OWNER, "me");
        olly2018.setField(StandardField.REVIEW, "review");
        olly2018.setField(StandardField.URL, "https://www.olly2018.edu");
        olly2018.setField(new UnknownField((DC_SOURCE)), "JabRef");

        xmpWriter.writeXmp(pdfFile, olly2018, null);
        assertFalse(xmpReader.readRawXmp(pdfFile).isEmpty(), "Metadata should exist before cleanup");

        LinkedFile linkedFile = new LinkedFile("Test PDF", pdfFile, "PDF");
        olly2018.addFile(linkedFile);
        when(databaseContext.getFileDirectories(any(FilePreferences.class)))
                .thenReturn(Collections.singletonList(tempDir));

        List<FieldChange> changes = cleanupJob.cleanup(olly2018);
        assertFalse(changes.isEmpty(), "Cleanup should report a change");
        assertEquals(StandardField.FILE, changes.getFirst().getField());
        assertTrue(xmpReader.readRawXmp(pdfFile).isEmpty(), "Metadata should be removed after cleanup");
    }

    @Test
    void cleanupDoesNothingIfNoMetadata() throws IOException {
        Path pdfFile = tempDir.resolve("test_no_metadata.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfFile.toFile());
        }
        assertTrue(xmpReader.readRawXmp(pdfFile).isEmpty(), "No metadata should exist initially");

        LinkedFile linkedFile = new LinkedFile("Test PDF", pdfFile, "PDF");
        BibEntry bibEntry = new BibEntry().withFiles(Collections.singletonList(linkedFile));

        List<FieldChange> changes = cleanupJob.cleanup(bibEntry);
        assertTrue(changes.isEmpty(), "Cleanup should report no changes for file without metadata");
    }
}
