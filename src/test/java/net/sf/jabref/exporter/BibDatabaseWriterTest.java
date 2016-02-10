package net.sf.jabref.exporter;

import com.google.common.base.Charsets;
import net.sf.jabref.*;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.structure.*;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.labelPattern.DatabaseLabelPattern;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
                Collections.emptyList(), mock(SavePreferences.class));
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullContextThrowsException() throws IOException {
        BibDatabaseWriter writer = new BibDatabaseWriter();
        writer.writePartOfDatabase(mock(Writer.class), null, Collections.emptyList(), new SavePreferences());
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullEntriesThrowsException() throws IOException {
        BibDatabaseWriter writer = new BibDatabaseWriter();
        writer.writePartOfDatabase(mock(Writer.class), new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX)),
                null, new SavePreferences());
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullPreferencesThrowsException() throws IOException {
        BibDatabaseWriter writer = new BibDatabaseWriter();
        writer.writePartOfDatabase(mock(Writer.class), new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX)),
                Collections.emptyList(), null);
    }

    @Test
    public void writeEncoding() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX)),
                Collections.emptyList(), preferences);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writePreamble() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibDatabase database = new BibDatabase();
        database.setPreamble("Test preamble");
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), new SavePreferences());

        Assert.assertEquals(Globals.NEWLINE + "@Preamble{Test preamble}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writePreambleAndEncoding() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibDatabase database = new BibDatabase();
        database.setPreamble("Test preamble");
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), preferences);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Preamble{Test preamble}" + Globals.NEWLINE, stringWriter.toString());
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
                new SavePreferences());

        Assert.assertEquals(Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEncodingAndEntry() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.singletonList(entry), preferences);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEpilogue() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibDatabase database = new BibDatabase();
        database.setEpilog("Test epilog");
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), new SavePreferences());

        Assert.assertEquals(Globals.NEWLINE + "Test epilog" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEpilogueAndEncoding() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibDatabase database = new BibDatabase();
        database.setEpilog("Test epilog");
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), preferences);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "Test epilog" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeMetadata() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        MetaData metaData = new MetaData();
        DatabaseLabelPattern labelPattern = new DatabaseLabelPattern();
        labelPattern.setDefaultValue("test");
        metaData.setLabelPattern(labelPattern);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), new SavePreferences());

        Assert.assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeMetadataAndEncoding() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        MetaData metaData = new MetaData();
        DatabaseLabelPattern labelPattern = new DatabaseLabelPattern();
        labelPattern.setDefaultValue("test");
        metaData.setLabelPattern(labelPattern);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), preferences);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeGroups() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        MetaData metaData = new MetaData();
        GroupTreeNode groupRoot = new GroupTreeNode(new AllEntriesGroup());
        groupRoot.add(new GroupTreeNode(new ExplicitGroup("test", GroupHierarchyType.INCLUDING)));
        metaData.setGroups(groupRoot);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), new SavePreferences());

        // @formatter:off
        Assert.assertEquals(Globals.NEWLINE +
                "@Comment{jabref-meta: groupsversion:3;}" + Globals.NEWLINE
                + Globals.NEWLINE
                + "@Comment{jabref-meta: groupstree:" + Globals.NEWLINE
                + "0 AllEntriesGroup:;" + Globals.NEWLINE
                + "1 ExplicitGroup:test\\;2\\;;" + Globals.NEWLINE
                + "}" + Globals.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    public void writeGroupsAndEncoding() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        MetaData metaData = new MetaData();
        GroupTreeNode groupRoot = new GroupTreeNode(new AllEntriesGroup());
        groupRoot.add(new GroupTreeNode(new ExplicitGroup("test", GroupHierarchyType.INCLUDING)));
        metaData.setGroups(groupRoot);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), preferences);

        // @formatter:off
        Assert.assertEquals(
                "% Encoding: US-ASCII" + Globals.NEWLINE +
                Globals.NEWLINE +
                "@Comment{jabref-meta: groupsversion:3;}" + Globals.NEWLINE
                + Globals.NEWLINE
                + "@Comment{jabref-meta: groupstree:" + Globals.NEWLINE
                + "0 AllEntriesGroup:;" + Globals.NEWLINE
                + "1 ExplicitGroup:test\\;2\\;;" + Globals.NEWLINE
                + "}" + Globals.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    public void writeString() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibDatabase database = new BibDatabase();
        database.addString(new BibtexString("id", "name", "content"));
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), new SavePreferences());

        Assert.assertEquals(Globals.NEWLINE + "@String{name = {content}}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeStringAndEncoding() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibDatabase database = new BibDatabase();
        database.addString(new BibtexString("id", "name", "content"));
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), preferences);

        Assert.assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@String{name = {content}}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEntryWithCustomizedTypeAlsoWritesTypeDeclaration() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        EntryTypes.addOrModifyCustomEntryType(new CustomEntryType("customizedType", "required", "optional"));
        BibEntry entry = new BibEntry();
        entry.setType("customizedType");
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.singletonList(entry),
                new SavePreferences());

        Assert.assertEquals(Globals.NEWLINE +
                        "@Customizedtype{," + Globals.NEWLINE + "}" + Globals.NEWLINE + Globals.NEWLINE
                        + "@Comment{jabref-entrytype: Customizedtype: req[required] opt[optional]}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void roundtrip() throws IOException {
        File testBibtexFile = new File("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormatReader.getReader(testBibtexFile, encoding));

        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));
        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, result.getDatabase().getEntries(), preferences);

        Assert.assertEquals(new Scanner(testBibtexFile).useDelimiter("\\A").next(), stringWriter.toString());
    }

    @Test
    public void writeSavedSerializationOfEntryIfUnchanged() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("presaved serialization");
        entry.setChanged(false);
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.singletonList(entry),
                new SavePreferences());

        Assert.assertEquals("presaved serialization", stringWriter.toString());
    }

    @Test
    public void reformatEntryIfAskedToDoSo() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("wrong serialization");
        entry.setChanged(false);
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        SavePreferences preferences = new SavePreferences().withReformatFile(true);
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.singletonList(entry), preferences);

        Assert.assertEquals(Globals.NEWLINE +
                        "@Article{," + Globals.NEWLINE + "  author = {Mr. author}" + Globals.NEWLINE + "}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void writeSavedSerializationOfStringIfUnchanged() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("id", "name", "content");
        string.setParsedSerialization("serialization");
        database.addString(string);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), new SavePreferences());

        Assert.assertEquals("serialization", stringWriter.toString());
    }

    @Test
    public void reformatStringIfAskedToDoSo() throws IOException {
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("id", "name", "content");
        string.setParsedSerialization("wrong serialization");
        database.addString(string);
        BibDatabaseContext context = new BibDatabaseContext(database, new Defaults(BibDatabaseMode.BIBTEX));

        SavePreferences preferences = new SavePreferences().withReformatFile(true);

        StringWriter stringWriter = new StringWriter();
        databaseWriter.writePartOfDatabase(stringWriter, context, Collections.emptyList(), preferences);

        Assert.assertEquals(Globals.NEWLINE + "@String{name = {content}}" + Globals.NEWLINE, stringWriter.toString());

    }
}