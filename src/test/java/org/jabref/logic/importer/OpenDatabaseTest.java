package org.jabref.logic.importer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenDatabaseTest {

    private final Charset defaultEncoding = StandardCharsets.UTF_8;
    private GeneralPreferences generalPreferences;
    private ImportFormatPreferences importFormatPreferences;
    private final Path bibNoHeader;
    private final Path bibWrongHeader;
    private final Path bibHeader;
    private final Path bibHeaderAndSignature;
    private final Path bibEncodingWithoutNewline;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    OpenDatabaseTest() throws URISyntaxException {
        bibNoHeader = Path.of(OpenDatabaseTest.class.getResource("headerless.bib").toURI());
        bibWrongHeader = Path.of(OpenDatabaseTest.class.getResource("wrong-header.bib").toURI());
        bibHeader = Path.of(OpenDatabaseTest.class.getResource("encoding-header.bib").toURI());
        bibHeaderAndSignature = Path.of(OpenDatabaseTest.class.getResource("jabref-header.bib").toURI());
        bibEncodingWithoutNewline = Path.of(OpenDatabaseTest.class.getResource("encodingWithoutNewline.bib").toURI());
    }

    @BeforeEach
    void setUp() {
        generalPreferences = mock(GeneralPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    void useFallbackEncodingIfNoHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibNoHeader, generalPreferences, importFormatPreferences, fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    void useFallbackEncodingIfUnknownHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibWrongHeader, generalPreferences, importFormatPreferences, fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    void useSpecifiedEncoding() throws IOException {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);
        ParserResult result = OpenDatabase.loadDatabase(bibHeader, generalPreferences, importFormatPreferences, fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    void useSpecifiedEncodingWithSignature() throws IOException {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);
        ParserResult result = OpenDatabase.loadDatabase(bibHeaderAndSignature, generalPreferences, importFormatPreferences, fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    void entriesAreParsedNoHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibNoHeader, generalPreferences, importFormatPreferences, fileMonitor);
        BibDatabase db = result.getDatabase();

        // Entry
        assertEquals(1, db.getEntryCount());
        assertEquals(Optional.of("2014"), db.getEntryByCitationKey("1").get().getField(StandardField.YEAR));
    }

    @Test
    void entriesAreParsedHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibHeader, generalPreferences, importFormatPreferences, fileMonitor);
        BibDatabase db = result.getDatabase();

        // Entry
        assertEquals(1, db.getEntryCount());
        assertEquals(Optional.of("2014"), db.getEntryByCitationKey("1").get().getField(StandardField.YEAR));
    }

    @Test
    void entriesAreParsedHeaderAndSignature() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibHeaderAndSignature, generalPreferences, importFormatPreferences, fileMonitor);
        BibDatabase db = result.getDatabase();

        // Entry
        assertEquals(1, db.getEntryCount());
        assertEquals(Optional.of("2014"), db.getEntryByCitationKey("1").get().getField(StandardField.YEAR));
    }

    /**
     * Test for #669
     */
    @Test
    void correctlyParseEncodingWithoutNewline() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibEncodingWithoutNewline, generalPreferences, importFormatPreferences, fileMonitor);
        assertEquals(StandardCharsets.US_ASCII, result.getMetaData().getEncoding().get());

        BibDatabase db = result.getDatabase();
        assertEquals(Optional.of("testPreamble"), db.getPreamble());

        Collection<BibEntry> entries = db.getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals(Optional.of("testArticle"), entry.getCitationKey());
    }
}
