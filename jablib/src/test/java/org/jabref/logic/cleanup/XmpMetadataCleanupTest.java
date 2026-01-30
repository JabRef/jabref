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
import org.jabref.model.database.BibDatabase;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmpMetadataCleanupTest {

    @TempDir
    Path tempDir;

    private BibDatabaseContext databaseContext;
    private XmpMetadataCleanup cleanupJob;
    private XmpUtilReader xmpReader;
    private XmpUtilWriter xmpWriter;
    private BibDatabase bibDatabase;

    @BeforeEach
    void setUp() {
        databaseContext = mock(BibDatabaseContext.class);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        bibDatabase = mock(BibDatabase.class);
        when(bibDatabase.resolveForStrings(anyList(), eq(false)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
        olly2018.withField(StandardField.AUTHOR, "Olly and Johannes")
                .withField(StandardField.TITLE, "Stefan's palace")
                .withField(StandardField.JOURNAL, "Test Journal")
                .withField(StandardField.VOLUME, "1")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "1-2")
                .withField(StandardField.ISSN, "978-123-123")
                .withField(StandardField.NOTE, "NOTE")
                .withField(StandardField.ABSTRACT, "ABSTRACT")
                .withField(StandardField.COMMENT, "COMMENT")
                .withField(StandardField.DOI, "10/3212.3123")
                .withField(StandardField.FILE, ":article_dublinCore.pdf:PDF")
                .withField(StandardField.GROUPS, "NO")
                .withField(StandardField.HOWPUBLISHED, "online")
                .withField(StandardField.KEYWORDS, "k1, k2")
                .withField(StandardField.OWNER, "me")
                .withField(StandardField.REVIEW, "review")
                .withField(StandardField.URL, "https://www.olly2018.edu")
                .withField(new UnknownField((DC_SOURCE)), "JabRef")
                .setMonth(Month.MARCH);
        xmpWriter.writeXmp(pdfFile, olly2018, bibDatabase);
        assertNotEquals(List.of(), xmpReader.readRawXmp(pdfFile));

        LinkedFile linkedFile = new LinkedFile("Test PDF", pdfFile, "PDF");
        olly2018.addFile(linkedFile);
        when(databaseContext.getFileDirectories(any(FilePreferences.class)))
                .thenReturn(Collections.singletonList(tempDir));

        List<FieldChange> changes = cleanupJob.cleanup(olly2018);
        List<FieldChange> expectedChanges = List.of(new FieldChange(
                olly2018,
                StandardField.FILE,
                // File field is present with entry, IntelliJ warning can be safely ignored
                olly2018.getField(StandardField.FILE).get(),
                olly2018.getField(StandardField.FILE).get()
        ));
        assertNotEquals(List.of(), changes);
        assertEquals(expectedChanges, changes);
        assertEquals(List.of(), xmpReader.readRawXmp(pdfFile));
    }

    @Test
    void cleanupDoesNothingIfNoMetadata() throws IOException {
        Path pdfFile = tempDir.resolve("test_no_metadata.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfFile.toFile());
        }
        assertEquals(List.of(), xmpReader.readRawXmp(pdfFile));

        LinkedFile linkedFile = new LinkedFile("Test PDF", pdfFile, "PDF");
        BibEntry bibEntry = new BibEntry().withFiles(Collections.singletonList(linkedFile));

        List<FieldChange> changes = cleanupJob.cleanup(bibEntry);
        assertEquals(List.of(), changes);
    }
}

