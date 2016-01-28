package net.sf.jabref.importer;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class OpenDatabaseActionTest {

    private final Charset defaultEncoding = StandardCharsets.UTF_8;
    private final File bibNoHeader = new File(OpenDatabaseActionTest.class.getResource("headerless.bib").getFile());
    private final File bibWrongHeader = new File(
            OpenDatabaseActionTest.class.getResource("wrong-header.bib").getFile());
    private final File bibHeader = new File(OpenDatabaseActionTest.class.getResource("encoding-header.bib").getFile());
    private final File bibHeaderAndSignature = new File(
            OpenDatabaseActionTest.class.getResource("jabref-header.bib").getFile());
    private final File bibEncodingWithoutNewline = new File(
            OpenDatabaseActionTest.class.getResource("encodingWithoutNewline.bib").getFile());

    @BeforeClass
    public static void setUpGlobalsPrefs() {
        // otherwise FieldContentParser (called by BibtexParser) crashes
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void useFallbackEncodingIfNoHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibNoHeader, defaultEncoding);
        Assert.assertEquals(defaultEncoding, result.getEncoding());
    }

    @Test
    public void useFallbackEncodingIfUnknownHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibWrongHeader, defaultEncoding);
        Assert.assertEquals(defaultEncoding, result.getEncoding());
    }

    @Test
    public void useSpecifiedEncoding() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeader, StandardCharsets.US_ASCII);
        Assert.assertEquals(StandardCharsets.UTF_8, result.getEncoding());
    }

    @Test
    public void useSpecifiedEncodingWithSignature() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeaderAndSignature, StandardCharsets.US_ASCII);
        Assert.assertEquals(StandardCharsets.UTF_8, result.getEncoding());
    }

    @Test
    public void entriesAreParsedNoHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibNoHeader, defaultEncoding);
        BibDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
    }

    @Test
    public void entriesAreParsedHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeader, defaultEncoding);
        BibDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
    }

    @Test
    public void entriesAreParsedHeaderAndSignature() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibHeaderAndSignature, defaultEncoding);
        BibDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
    }

    /**
     * Test for #669
     */
    @Test
    public void correctlyParseEncodingWithoutNewline() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(bibEncodingWithoutNewline, defaultEncoding);
        Assert.assertEquals(StandardCharsets.US_ASCII, result.getEncoding());

        BibDatabase db = result.getDatabase();
        Assert.assertEquals("testPreamble", db.getPreamble());

        Collection<BibEntry> entries = db.getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("testArticle", entry.getCiteKey());
    }
}