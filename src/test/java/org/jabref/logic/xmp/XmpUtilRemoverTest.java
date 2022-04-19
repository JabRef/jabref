package org.jabref.logic.xmp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.jabref.logic.xmp.DublinCoreExtractor.DC_COVERAGE;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_RIGHTS;
import static org.jabref.logic.xmp.DublinCoreExtractor.DC_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XmpUtilRemoverTest {
    private static BibEntry olly2018;
    private static BibEntry toral2006;
    private static BibEntry vapnik2000;
    private XmpPreferences xmpPreferences;

    private void initBibEntries() {
        olly2018 = new BibEntry(StandardEntryType.Article);
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

        toral2006 = new BibEntry(StandardEntryType.InProceedings);
        toral2006.setField(StandardField.AUTHOR, "Toral, Antonio and Munoz, Rafael");
        toral2006.setField(StandardField.TITLE, "A proposal to automatically build and maintain gazetteers for Named Entity Recognition by using Wikipedia");
        toral2006.setField(StandardField.BOOKTITLE, "Proceedings of EACL");
        toral2006.setField(StandardField.PAGES, "56--61");
        toral2006.setField(StandardField.EPRINTTYPE, "asdf");
        toral2006.setField(StandardField.OWNER, "Ich");
        toral2006.setField(StandardField.URL, "www.url.de");

        vapnik2000 = new BibEntry(StandardEntryType.Book);
        vapnik2000.setCitationKey("vapnik2000");
        vapnik2000.setField(StandardField.TITLE, "The Nature of Statistical Learning Theory");
        vapnik2000.setField(StandardField.PUBLISHER, "Springer Science + Business Media");
        vapnik2000.setField(StandardField.AUTHOR, "Vladimir N. Vapnik");
        vapnik2000.setField(StandardField.DOI, "10.1007/978-1-4757-3264-1");
        vapnik2000.setField(StandardField.OWNER, "Ich");
        vapnik2000.setField(StandardField.LANGUAGE, "English, Japanese");
        vapnik2000.setDate(new Date(2000, 5));

        vapnik2000.setField(new UnknownField(DC_COVERAGE), "coverageField");
        vapnik2000.setField(new UnknownField((DC_SOURCE)), "JabRef");
        vapnik2000.setField(new UnknownField(DC_RIGHTS), "Right To X");
    }

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @BeforeEach
    void setUp() {
        xmpPreferences = mock(XmpPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(false);
        when(xmpPreferences.getXmpPrivacyFilter()).thenReturn(FXCollections.observableSet(new HashSet<>()));
        when(xmpPreferences.getSelectAllFields()).thenReturn(new SimpleBooleanProperty(false));
        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        this.initBibEntries();
    }

    @Test
    void testDeleteXmpSingleField() throws IOException, URISyntaxException {
        Path tempFile = Files.createTempFile("JabRef", "pdf");
        Path pathPdf = Path.of((XmpUtilShared.class.getResource("PD_metadata.pdf").toURI()));
        // copy data to temp file for testing
        FileUtil.copyFile(pathPdf, tempFile, true);

        // entries in odc
        List<BibEntry> originalEntries = XmpUtilReader.readXmp(pathPdf.toAbsolutePath().toString(), xmpPreferences);
        BibEntry expectedEntry = originalEntries.get(0);

        // turn filters on
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(true);
        Set<Field> fields = new HashSet<>();
        fields.add(StandardField.AUTHOR);
        // filter author field
        when(xmpPreferences.getXmpPrivacyFilter()).thenReturn(FXCollections.observableSet(fields));

        // delete author field from the PDF xmp metadata
        try {
            XmpUtilRemover.deleteXmp(tempFile, expectedEntry, null, xmpPreferences);
            List<BibEntry> modifiedEntries = XmpUtilReader.readXmp(tempFile.toAbsolutePath().toString(), xmpPreferences);
            BibEntry modifiedEntry = modifiedEntries.get(0);

            expectedEntry.clearField(StandardField.AUTHOR);

            // not comparing temp file and original file
            expectedEntry.clearField(StandardField.FILE);
            modifiedEntry.clearField(StandardField.FILE);

            // assert
            assertEquals(expectedEntry, modifiedEntry);
        } catch (IOException | TransformerException e) {
            fail();
        }
    }

    @Test
    void testDeleteXmpAllFields() throws IOException, URISyntaxException {
        Path tempFile = Files.createTempFile("JabRef", "pdf");
        Path pathPdf = Path.of((XmpUtilShared.class.getResource("PD_metadata.pdf").toURI()));
        // copy data to temp file for testing
        FileUtil.copyFile(pathPdf, tempFile, true);

        List<BibEntry> originalEntries = XmpUtilReader.readXmp(pathPdf.toAbsolutePath().toString(), xmpPreferences);
        BibEntry expectedEntry = originalEntries.get(0);

        // turn filter on
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(true);
        // turn select-all-fields on
        when(xmpPreferences.getSelectAllFields()).thenReturn(new SimpleBooleanProperty(true));

        // delete all xmp metadata fields
        try {
            XmpUtilRemover.deleteXmp(tempFile, expectedEntry, null, xmpPreferences);
            List<BibEntry> modifiedEntries = XmpUtilReader.readXmp(tempFile.toAbsolutePath().toString(), xmpPreferences);

            // assert
            assertEquals(0, modifiedEntries.size());
        } catch (IOException | TransformerException e) {
            fail();
        }
    }

    @Test
    void testDeleteXmpWithMultipleEntries(@TempDir Path tempDir) throws IOException {
        List<BibEntry> entries = Arrays.asList(olly2018, vapnik2000, toral2006);
        Path tempFile = this.createDefaultFile("banana.pdf", tempDir);
        try {
            XmpUtilWriter.writeXmp(Path.of(tempFile.toAbsolutePath().toString()), entries, null, xmpPreferences);
        } catch (TransformerException e) {
            fail();
        }

        List<BibEntry> expectedEntries = XmpUtilReader.readXmp(tempFile.toAbsolutePath().toString(), xmpPreferences);

        // turn filters on
        when(xmpPreferences.shouldUseXmpPrivacyFilter()).thenReturn(true);
        Set<Field> fields = new HashSet<>();
        fields.add(StandardField.AUTHOR);
        // filter author field
        when(xmpPreferences.getXmpPrivacyFilter()).thenReturn(FXCollections.observableSet(fields));

        // delete all author field from all entries in metadata
        try {
            XmpUtilRemover.deleteXmp(tempFile, expectedEntries, null, xmpPreferences);
            List<BibEntry> modifiedEntries = XmpUtilReader.readXmp(tempFile.toAbsolutePath().toString(), xmpPreferences);

            for (int i = 0; i < modifiedEntries.size(); i++) {
                expectedEntries.get(i).clearField(StandardField.AUTHOR);
                expectedEntries.get(i).clearField(StandardField.FILE);
                modifiedEntries.get(i).clearField(StandardField.FILE);
                assertEquals(expectedEntries.get(i), modifiedEntries.get(i));
            }

        } catch (IOException | TransformerException e) {
            assertTrue(false);
        }
    }

    private Path createDefaultFile(String fileName, Path tempDir) throws IOException {
        // create a default PDF
        Path pdfFile = tempDir.resolve(fileName);
        try (PDDocument pdf = new PDDocument()) {
            // Need a single page to open in Acrobat
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.toAbsolutePath().toString());
        }

        return pdfFile;
    }
}
