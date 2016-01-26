package net.sf.jabref.exporter;

import com.google.common.base.Charsets;
import net.sf.jabref.*;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.io.IOUtil;
import sun.misc.IOUtils;

import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Scanner;

import static org.junit.Assert.*;

public class BibDatabaseWriterTest {

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullWriterThrowsException() throws IOException {
        BibDatabaseWriter writer = new BibDatabaseWriter();
        writer.writePartOfDatabase(null, new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX)),
                Collections.emptyList(), mock(SavePreferences.class), false, false);
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullContextThrowsException() throws IOException {
        BibDatabaseWriter writer = new BibDatabaseWriter();
        writer.writePartOfDatabase(mock(Writer.class), null, Collections.emptyList(), new SavePreferences(), false,
                false);
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullEntriesThrowsException() throws IOException {
        BibDatabaseWriter writer = new BibDatabaseWriter();
        writer.writePartOfDatabase(mock(Writer.class), new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX)),
                null, new SavePreferences(), false, false);
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullPreferencesThrowsException() throws IOException {
        BibDatabaseWriter writer = new BibDatabaseWriter();
        writer.writePartOfDatabase(mock(Writer.class), new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX)),
                Collections.emptyList(), null, false, false);
    }

    @Test
    public void writeEncoding() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences();
        preferences.setEncoding(Charsets.US_ASCII);

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX)),
                Collections.emptyList(), preferences, false, false);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writePreamble() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibDatabase database = new BibDatabase();
        database.setPreamble("Test preamble");
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), new SavePreferences(), false,
                false);

        Assert.assertEquals(Globals.NEWLINE + "@PREAMBLE{Test preamble}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writePreambleAndEncoding() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences();
        preferences.setEncoding(Charsets.US_ASCII);
        BibDatabase database = new BibDatabase();
        database.setPreamble("Test preamble");
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), preferences, false, false);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@PREAMBLE{Test preamble}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEntry() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.singletonList(entry),
                new SavePreferences(), false, false);

        Assert.assertEquals(Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEncodingAndEntry() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences();
        preferences.setEncoding(Charsets.US_ASCII);
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.singletonList(entry), preferences, false,
                false);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void roundtrip() throws IOException {
        File testBibtexFile = new File("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormatReader.getReader(testBibtexFile, encoding));

        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences();
        preferences.setEncoding(encoding);
        preferences.setSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));
        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, result.getDatabase().getEntries(), preferences, false,
                false);

        Assert.assertEquals(new Scanner(testBibtexFile).useDelimiter("\\A").next(), stringWriter.toString());
    }
}