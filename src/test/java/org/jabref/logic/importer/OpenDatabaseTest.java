package org.jabref.logic.importer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenDatabaseTest {

    private final Charset defaultEncoding = StandardCharsets.UTF_8;
    private ImportFormatPreferences importFormatPreferences;
    private final File bibNoHeader;
    private final File bibWrongHeader;
    private final File bibHeader;
    private final File bibHeaderAndSignature;
    private final File bibEncodingWithoutNewline;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    public OpenDatabaseTest() throws URISyntaxException {
        bibNoHeader = Paths.get(OpenDatabaseTest.class.getResource("headerless.bib").toURI()).toFile();
        bibWrongHeader = Paths.get(OpenDatabaseTest.class.getResource("wrong-header.bib").toURI()).toFile();
        bibHeader = Paths.get(OpenDatabaseTest.class.getResource("encoding-header.bib").toURI()).toFile();
        bibHeaderAndSignature = Paths.get(OpenDatabaseTest.class.getResource("jabref-header.bib").toURI())
                .toFile();
        bibEncodingWithoutNewline = Paths
                .get(OpenDatabaseTest.class.getResource("encodingWithoutNewline.bib").toURI()).toFile();
    }

    @BeforeEach
    public void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
    }

    @Test
    public void useFallbackEncodingIfNoHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibNoHeader, importFormatPreferences, fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    public void useFallbackEncodingIfUnknownHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibWrongHeader, importFormatPreferences, fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    public void useSpecifiedEncoding() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibHeader,
                importFormatPreferences.withEncoding(StandardCharsets.US_ASCII), fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    public void useSpecifiedEncodingWithSignature() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibHeaderAndSignature,
                importFormatPreferences.withEncoding(StandardCharsets.US_ASCII), fileMonitor);
        assertEquals(defaultEncoding, result.getMetaData().getEncoding().get());
    }

    @Test
    public void entriesAreParsedNoHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibNoHeader, importFormatPreferences, fileMonitor);
        BibDatabase db = result.getDatabase();

        // Entry
        assertEquals(1, db.getEntryCount());
        assertEquals(Optional.of("2014"), db.getEntryByKey("1").get().getField("year"));
    }

    @Test
    public void entriesAreParsedHeader() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibHeader, importFormatPreferences, fileMonitor);
        BibDatabase db = result.getDatabase();

        // Entry
        assertEquals(1, db.getEntryCount());
        assertEquals(Optional.of("2014"), db.getEntryByKey("1").get().getField("year"));
    }

    @Test
    public void entriesAreParsedHeaderAndSignature() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibHeaderAndSignature, importFormatPreferences, fileMonitor);
        BibDatabase db = result.getDatabase();

        // Entry
        assertEquals(1, db.getEntryCount());
        assertEquals(Optional.of("2014"), db.getEntryByKey("1").get().getField("year"));
    }

    /**
     * Test for #669
     */
    @Test
    public void correctlyParseEncodingWithoutNewline() throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(bibEncodingWithoutNewline, importFormatPreferences, fileMonitor);
        assertEquals(StandardCharsets.US_ASCII, result.getMetaData().getEncoding().get());

        BibDatabase db = result.getDatabase();
        assertEquals(Optional.of("testPreamble"), db.getPreamble());

        Collection<BibEntry> entries = db.getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals(Optional.of("testArticle"), entry.getCiteKeyOptional());
    }
}
