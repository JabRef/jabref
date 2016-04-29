package net.sf.jabref.exporter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import net.sf.jabref.logic.groups.AllEntriesGroup;
import net.sf.jabref.logic.groups.ExplicitGroup;
import net.sf.jabref.logic.groups.GroupHierarchyType;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.labelpattern.AbstractLabelPattern;
import net.sf.jabref.logic.labelpattern.DatabaseLabelPattern;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.IdGenerator;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class BibDatabaseWriterTest {

    private BibDatabaseWriter databaseWriter;
    private StringWriter stringWriter;
    private BibDatabase database;
    private MetaData metaData;
    private BibDatabaseContext bibtexContext;

    @BeforeClass
    public static void setUpClass() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUp() {
        databaseWriter = new BibDatabaseWriter();
        stringWriter = new StringWriter();
        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData, new Defaults(BibDatabaseMode.BIBTEX));
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullWriterThrowsException() throws IOException {
        databaseWriter.writePartOfDatabase(null, bibtexContext, Collections.emptyList(), mock(SavePreferences.class));
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullContextThrowsException() throws IOException {
        databaseWriter.writePartOfDatabase(mock(Writer.class), null, Collections.emptyList(), new SavePreferences());
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullEntriesThrowsException() throws IOException {
        databaseWriter.writePartOfDatabase(mock(Writer.class), bibtexContext, null, new SavePreferences());
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullPreferencesThrowsException() throws IOException {
        databaseWriter.writePartOfDatabase(mock(Writer.class), bibtexContext, Collections.emptyList(), null);
    }

    @Test
    public void writeEncoding() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writePreamble() throws IOException {
        database.setPreamble("Test preamble");

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Preamble{Test preamble}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writePreambleAndEncoding() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        database.setPreamble("Test preamble");

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Preamble{Test preamble}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEntry() throws IOException {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.singletonList(entry),
                new SavePreferences());

        assertEquals(Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}" + Globals.NEWLINE + Globals.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEncodingAndEntry() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.singletonList(entry), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}"
                + Globals.NEWLINE + Globals.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEpilogue() throws IOException {
        database.setEpilog("Test epilog");

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "Test epilog" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEpilogueAndEncoding() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        database.setEpilog("Test epilog");

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "Test epilog" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeMetadata() throws IOException {
        DatabaseLabelPattern labelPattern = new DatabaseLabelPattern();
        labelPattern.setDefaultValue("test");
        metaData.setLabelPattern(labelPattern);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void writeMetadataAndEncoding() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        DatabaseLabelPattern labelPattern = new DatabaseLabelPattern();
        labelPattern.setDefaultValue("test");
        metaData.setLabelPattern(labelPattern);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE
                +
                "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeGroups() throws IOException, ParseException {
        GroupTreeNode groupRoot = new GroupTreeNode(new AllEntriesGroup());
        groupRoot.addSubgroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING));
        metaData.setGroups(groupRoot);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        // @formatter:off
        assertEquals(Globals.NEWLINE
                + "@Comment{jabref-meta: groupstree:" + Globals.NEWLINE
                + "0 AllEntriesGroup:;" + Globals.NEWLINE
                + "1 ExplicitGroup:test\\;2\\;;" + Globals.NEWLINE
                + "}" + Globals.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    public void writeGroupsAndEncoding() throws IOException, ParseException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);

        GroupTreeNode groupRoot = new GroupTreeNode(new AllEntriesGroup());
        groupRoot.addChild(new GroupTreeNode(new ExplicitGroup("test", GroupHierarchyType.INCLUDING)));
        metaData.setGroups(groupRoot);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), preferences);

        // @formatter:off
        assertEquals(
                "% Encoding: US-ASCII" + Globals.NEWLINE +
                Globals.NEWLINE
                + "@Comment{jabref-meta: groupstree:" + Globals.NEWLINE
                + "0 AllEntriesGroup:;" + Globals.NEWLINE
                + "1 ExplicitGroup:test\\;2\\;;" + Globals.NEWLINE
                + "}" + Globals.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    public void writeString() throws IOException {
        database.addString(new BibtexString("id", "name", "content"));

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@String{name = {content}}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeStringAndEncoding() throws IOException {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        database.addString(new BibtexString("id", "name", "content"));

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@String{name = {content}}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeEntryWithCustomizedTypeAlsoWritesTypeDeclaration() throws IOException {
        EntryTypes.addOrModifyCustomEntryType(new CustomEntryType("customizedType", "required", "optional"));
        BibEntry entry = new BibEntry();
        entry.setType("customizedType");
        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.singletonList(entry),
                new SavePreferences());

        assertEquals(
                Globals.NEWLINE +
                        "@Customizedtype{," + Globals.NEWLINE + "}" + Globals.NEWLINE + Globals.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + Globals.NEWLINE + Globals.NEWLINE
                        + "@Comment{jabref-entrytype: Customizedtype: req[required] opt[optional]}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void roundtrip() throws IOException {
        File testBibtexFile = Paths.get("src/test/resources/testbib/complex.bib").toFile();
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormatReader.getReader(testBibtexFile, encoding));

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.writePartOfDatabase(stringWriter, context, result.getDatabase().getEntries(), preferences);
        try (Scanner scanner = new Scanner(testBibtexFile,encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    public void writeSavedSerializationOfEntryIfUnchanged() throws IOException {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("presaved serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.singletonList(entry),
                new SavePreferences());

        assertEquals("presaved serialization" + Globals.NEWLINE + "@Comment{jabref-meta: databaseType:bibtex;}"
                + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void reformatEntryIfAskedToDoSo() throws IOException {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("wrong serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        SavePreferences preferences = new SavePreferences().withReformatFile(true);
        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.singletonList(entry), preferences);

        assertEquals(Globals.NEWLINE +
                        "@Article{," + Globals.NEWLINE + "  author = {Mr. author}," + Globals.NEWLINE + "}"
                        + Globals.NEWLINE + Globals.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void writeSavedSerializationOfStringIfUnchanged() throws IOException {
        BibtexString string = new BibtexString("id", "name", "content");
        string.setParsedSerialization("serialization");
        database.addString(string);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals("serialization", stringWriter.toString());
    }

    @Test
    public void reformatStringIfAskedToDoSo() throws IOException {
        BibtexString string = new BibtexString("id", "name", "content");
        string.setParsedSerialization("wrong serialization");
        database.addString(string);

        SavePreferences preferences = new SavePreferences().withReformatFile(true);
        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), preferences);

        assertEquals(Globals.NEWLINE + "@String{name = {content}}" + Globals.NEWLINE, stringWriter.toString());

    }

    @Test
    public void writeSaveActions() throws Exception {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: saveActions:enabled;" + Globals.NEWLINE
                + "title[lower_case]" + Globals.NEWLINE + ";}" + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeSaveOrderConfig() throws Exception {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(false, new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true),
                new SaveOrderConfig.SortCriterion("abstract", false));
        metaData.setSaveOrderConfig(saveOrderConfig);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE
                + "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"
                + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeCustomKeyPattern() throws Exception {
        AbstractLabelPattern pattern = new DatabaseLabelPattern();
        pattern.setDefaultValue("test");
        pattern.addLabelPattern("article", "articleTest");
        metaData.setLabelPattern(pattern);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: keypattern_article:articleTest;}" + Globals.NEWLINE
                        + Globals.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void writeBiblatexMode() throws Exception {
        metaData.setMode(BibDatabaseMode.BIBLATEX);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: databaseType:biblatex;}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void writeProtectedFlag() throws Exception {
        metaData.markAsProtected();

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: protectedFlag:true;}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void writeContentSelectors() throws Exception {
        metaData.setContentSelectors("title", Arrays.asList("testWord", "word2"));

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: selector_title:testWord;word2;}" + Globals.NEWLINE,
                stringWriter.toString());
    }

    @Test
    public void writeFileDirectories() throws Exception {
        metaData.setDefaultFileDirectory("\\Literature\\");
        metaData.setUserFileDirectory("defaultOwner-user", "D:\\Documents");

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}" + Globals.NEWLINE +
                Globals.NEWLINE + "@Comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                + Globals.NEWLINE, stringWriter.toString());
    }

    @Test
    public void writeNotEmptyContentSelectors() throws Exception {
        metaData.setContentSelectors("title", Collections.singletonList(""));

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals("", stringWriter.toString());
    }

    @Test
    public void writeNotCompletelyEmptyContentSelectors() throws Exception {
        metaData.setContentSelectors("title", Collections.emptyList());

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals("", stringWriter.toString());
    }

    @Test
    public void writeEntriesSorted() throws IOException {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(false, new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true),
                new SaveOrderConfig.SortCriterion("abstract", false));
        metaData.setSaveOrderConfig(saveOrderConfig);

        BibEntry firstEntry = new BibEntry();
        firstEntry.setType(BibtexEntryTypes.ARTICLE);
        firstEntry.setField("author", "A");
        firstEntry.setField("year", "2000");

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType(BibtexEntryTypes.ARTICLE);
        secondEntry.setField("author", "A");
        secondEntry.setField("year", "2010");

        BibEntry thirdEntry = new BibEntry();
        thirdEntry.setType(BibtexEntryTypes.ARTICLE);
        thirdEntry.setField("author", "B");
        thirdEntry.setField("year", "2000");

        database.insertEntry(secondEntry);
        database.insertEntry(thirdEntry);
        database.insertEntry(firstEntry);

        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, database.getEntries(), new SavePreferences());

        assertEquals(
                Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE +
                "  author = {A}," + Globals.NEWLINE +
                "  year   = {2000}," + Globals.NEWLINE +
                "}"  + Globals.NEWLINE + Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE +
                "  author = {A}," + Globals.NEWLINE +
                "  year   = {2010}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE + Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE +
                "  author = {B}," + Globals.NEWLINE +
                "  year   = {2000}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE + Globals.NEWLINE +
                "@Comment{jabref-meta: databaseType:bibtex;}"
                 + Globals.NEWLINE + Globals.NEWLINE +
                "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}" +
                Globals.NEWLINE
                , stringWriter.toString());
    }

    @Test
    public void writeEntriesInOriginalOrderWhenNoSaveOrderConfigIsSetInMetadata() throws IOException {
        BibEntry firstEntry = new BibEntry(IdGenerator.next());
        firstEntry.setType(BibtexEntryTypes.ARTICLE);
        firstEntry.setField("author", "A");
        firstEntry.setField("year", "2010");

        BibEntry secondEntry = new BibEntry(IdGenerator.next());
        secondEntry.setType(BibtexEntryTypes.ARTICLE);
        secondEntry.setField("author", "B");
        secondEntry.setField("year", "2000");

        BibEntry thirdEntry = new BibEntry(IdGenerator.next());
        thirdEntry.setType(BibtexEntryTypes.ARTICLE);
        thirdEntry.setField("author", "A");
        thirdEntry.setField("year", "2000");

        database.insertEntry(firstEntry);
        database.insertEntry(secondEntry);
        database.insertEntry(thirdEntry);

        SavePreferences preferences = new SavePreferences().withSaveInOriginalOrder(false);
        databaseWriter.writePartOfDatabase(stringWriter, bibtexContext, database.getEntries(), preferences);

        assertEquals(
                Globals.NEWLINE +
                        "@Article{," + Globals.NEWLINE +
                        "  author = {A}," + Globals.NEWLINE +
                        "  year   = {2010}," + Globals.NEWLINE +
                        "}" + Globals.NEWLINE + Globals.NEWLINE +
                        "@Article{," + Globals.NEWLINE +
                        "  author = {B}," + Globals.NEWLINE +
                        "  year   = {2000}," + Globals.NEWLINE +
                        "}" + Globals.NEWLINE + Globals.NEWLINE +
                        "@Article{," + Globals.NEWLINE +
                        "  author = {A}," + Globals.NEWLINE +
                        "  year   = {2000}," + Globals.NEWLINE +
                        "}"
                        + Globals.NEWLINE + Globals.NEWLINE +
                        "@Comment{jabref-meta: databaseType:bibtex;}"
                        + Globals.NEWLINE
                , stringWriter.toString());
    }
}
