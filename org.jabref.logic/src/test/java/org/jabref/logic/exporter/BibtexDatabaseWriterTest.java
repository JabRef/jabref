package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Scanner;

import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.util.OS;
import org.jabref.model.Defaults;
import org.jabref.model.EntryTypes;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class BibtexDatabaseWriterTest {

    private BibtexDatabaseWriter<StringSaveSession> databaseWriter;
    private BibDatabase database;
    private MetaData metaData;
    private BibDatabaseContext bibtexContext;
    private ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    @BeforeEach
    public void setUp() {
        // Write to a string instead of to a file
        databaseWriter = new BibtexDatabaseWriter<>(StringSaveSession::new);

        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData, new Defaults(BibDatabaseMode.BIBTEX));
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    public void writeWithNullContextThrowsException() throws Exception {
        assertThrows(NullPointerException.class, () -> databaseWriter.savePartOfDatabase(null, Collections.emptyList(), new SavePreferences()));
    }

    @Test
    public void writeWithNullEntriesThrowsException() throws Exception {
        assertThrows(NullPointerException.class, () -> databaseWriter.savePartOfDatabase(bibtexContext, null, new SavePreferences()));
    }

    @Test
    public void writeWithNullPreferencesThrowsException() throws Exception {
        assertThrows(NullPointerException.class, () -> databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), null));
    }

    @Test
    public void writeEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.US_ASCII);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writePreamble() throws Exception {
        database.setPreamble("Test preamble");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@Preamble{Test preamble}" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writePreambleAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.US_ASCII);
        database.setPreamble("Test preamble");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@Preamble{Test preamble}" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEntry() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), new SavePreferences());

        assertEquals(OS.NEWLINE +
                "@Article{," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEncodingAndEntry() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.US_ASCII);
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), preferences);

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@Article{," + OS.NEWLINE + "}"
                + OS.NEWLINE + OS.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEpilogue() throws Exception {
        database.setEpilog("Test epilog");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "Test epilog" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEpilogueAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.US_ASCII);
        database.setEpilog("Test epilog");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "Test epilog" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeMetadata() throws Exception {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(mock(GlobalBibtexKeyPattern.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeMetadataAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.US_ASCII);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(mock(GlobalBibtexKeyPattern.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE
                +
                "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeGroups() throws Exception {
        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addSubgroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ','));
        metaData.setGroups(groupRoot);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        // @formatter:off
        assertEquals(OS.NEWLINE
                + "@Comment{jabref-meta: grouping:" + OS.NEWLINE
                + "0 AllEntriesGroup:;" + OS.NEWLINE
                + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + OS.NEWLINE
                + "}" + OS.NEWLINE, session.getStringValue());
        // @formatter:on
    }

    @Test
    public void writeGroupsAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.US_ASCII);

        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addChild(GroupTreeNode.fromGroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ',')));
        metaData.setGroups(groupRoot);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        // @formatter:off
        assertEquals(
                "% Encoding: US-ASCII" + OS.NEWLINE +
                OS.NEWLINE
                        + "@Comment{jabref-meta: grouping:" + OS.NEWLINE
                + "0 AllEntriesGroup:;" + OS.NEWLINE
                        + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + OS.NEWLINE
                + "}" + OS.NEWLINE, session.getStringValue());
        // @formatter:on
    }

    @Test
    public void writeString() throws Exception {
        database.addString(new BibtexString("name", "content"));

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@String{name = {content}}" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeStringAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.US_ASCII);
        database.addString(new BibtexString("name", "content"));

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@String{name = {content}}" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEntryWithCustomizedTypeAlsoWritesTypeDeclaration() throws Exception {
        try {
            EntryTypes.addOrModifyCustomEntryType(new CustomEntryType("customizedType", "required", "optional"), BibDatabaseMode.BIBTEX);
            BibEntry entry = new BibEntry();
            entry.setType("customizedType");
            database.insertEntry(entry);

            StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), new SavePreferences());

            assertEquals(
                    OS.NEWLINE +
                            "@Customizedtype{," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                            + "@Comment{jabref-meta: databaseType:bibtex;}"
                            + OS.NEWLINE + OS.NEWLINE
                            + "@Comment{jabref-entrytype: Customizedtype: req[required] opt[optional]}" + OS.NEWLINE,
                    session.getStringValue());
        } finally {
            EntryTypes.removeAllCustomEntryTypes();
        }
    }

    @Test
    public void roundtrip() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        StringSaveSession session = databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries(), preferences);
        try (Scanner scanner = new Scanner(testBibtexFile,encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), session.getStringValue());
        }
    }

    @Test
    public void roundtripWithUserComment() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        StringSaveSession session = databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries(), preferences);
        try (Scanner scanner = new Scanner(testBibtexFile,encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), session.getStringValue());
        }
    }

    @Test
    public void roundtripWithUserCommentAndEntryChange() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        BibEntry entry = result.getDatabase().getEntryByKey("1137631").get();
        entry.setField("author", "Mr. Author");

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        StringSaveSession session = databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries(), preferences);

        try (Scanner scanner = new Scanner(Paths.get("src/test/resources/testbib/bibWithUserCommentAndEntryChange.bib"),encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), session.getStringValue());
        }
    }

    @Test
    public void roundtripWithUserCommentBeforeStringAndChange() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        for (BibtexString string : result.getDatabase().getStringValues()) {
            // Mark them as changed
            string.setContent(string.getContent());
        }

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        StringSaveSession session = databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries(), preferences);

        try (Scanner scanner = new Scanner(testBibtexFile,encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), session.getStringValue());
        }
    }

    @Test
    public void roundtripWithUnknownMetaData() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/unknownMetaData.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        StringSaveSession session = databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries(), preferences);
        try (Scanner scanner = new Scanner(testBibtexFile,encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), session.getStringValue());
        }
    }

    @Test
    public void writeSavedSerializationOfEntryIfUnchanged() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("presaved serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), new SavePreferences());

        assertEquals("presaved serialization" + OS.NEWLINE + "@Comment{jabref-meta: databaseType:bibtex;}"
                + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void reformatEntryIfAskedToDoSo() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("wrong serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        SavePreferences preferences = new SavePreferences().withReformatFile(true);
        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), preferences);

        assertEquals(OS.NEWLINE +
                        "@Article{," + OS.NEWLINE + "  author = {Mr. author}," + OS.NEWLINE + "}"
                        + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + OS.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeSavedSerializationOfStringIfUnchanged() throws Exception {
        BibtexString string = new BibtexString("name", "content");
        string.setParsedSerialization("serialization");
        database.addString(string);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals("serialization", session.getStringValue());
    }

    @Test
    public void reformatStringIfAskedToDoSo() throws Exception {
        BibtexString string = new BibtexString("name", "content");
        string.setParsedSerialization("wrong serialization");
        database.addString(string);

        SavePreferences preferences = new SavePreferences().withReformatFile(true);
        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals(OS.NEWLINE + "@String{name = {content}}" + OS.NEWLINE, session.getStringValue());

    }

    @Test
    public void writeSaveActions() throws Exception {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: saveActions:enabled;" + OS.NEWLINE
                + "title[lower_case]" + OS.NEWLINE + ";}" + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeSaveOrderConfig() throws Exception {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(false, new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true),
                new SaveOrderConfig.SortCriterion("abstract", false));
        metaData.setSaveOrderConfig(saveOrderConfig);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE
                + "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"
                + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeCustomKeyPattern() throws Exception {
        AbstractBibtexKeyPattern pattern = new DatabaseBibtexKeyPattern(mock(GlobalBibtexKeyPattern.class));
        pattern.setDefaultValue("test");
        pattern.addBibtexKeyPattern("article", "articleTest");
        metaData.setCiteKeyPattern(pattern);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: keypattern_article:articleTest;}" + OS.NEWLINE
                        + OS.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeBiblatexMode() throws Exception {
        metaData.setMode(BibDatabaseMode.BIBLATEX);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: databaseType:biblatex;}" + OS.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeProtectedFlag() throws Exception {
        metaData.markAsProtected();

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: protectedFlag:true;}" + OS.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeFileDirectories() throws Exception {
        metaData.setDefaultFileDirectory("\\Literature\\");
        metaData.setUserFileDirectory("defaultOwner-user", "D:\\Documents");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}" + OS.NEWLINE +
                OS.NEWLINE + "@Comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                + OS.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEntriesSorted() throws Exception {
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

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries(), new SavePreferences());

        assertEquals(
                OS.NEWLINE +
                "@Article{," + OS.NEWLINE +
                "  author = {A}," + OS.NEWLINE +
                "  year   = {2000}," + OS.NEWLINE +
                "}"  + OS.NEWLINE + OS.NEWLINE +
                "@Article{," + OS.NEWLINE +
                "  author = {A}," + OS.NEWLINE +
                "  year   = {2010}," + OS.NEWLINE +
                "}" + OS.NEWLINE + OS.NEWLINE +
                "@Article{," + OS.NEWLINE +
                "  author = {B}," + OS.NEWLINE +
                "  year   = {2000}," + OS.NEWLINE +
                "}" + OS.NEWLINE + OS.NEWLINE +
                "@Comment{jabref-meta: databaseType:bibtex;}"
                 + OS.NEWLINE + OS.NEWLINE +
                "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}" +
                OS.NEWLINE
                , session.getStringValue());
    }

    @Test
    public void writeEntriesInOriginalOrderWhenNoSaveOrderConfigIsSetInMetadata() throws Exception {
        BibEntry firstEntry = new BibEntry();
        firstEntry.setType(BibtexEntryTypes.ARTICLE);
        firstEntry.setField("author", "A");
        firstEntry.setField("year", "2010");

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType(BibtexEntryTypes.ARTICLE);
        secondEntry.setField("author", "B");
        secondEntry.setField("year", "2000");

        BibEntry thirdEntry = new BibEntry();
        thirdEntry.setType(BibtexEntryTypes.ARTICLE);
        thirdEntry.setField("author", "A");
        thirdEntry.setField("year", "2000");

        database.insertEntry(firstEntry);
        database.insertEntry(secondEntry);
        database.insertEntry(thirdEntry);

        SavePreferences preferences = new SavePreferences().withSaveInOriginalOrder(false);
        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries(), preferences);

        assertEquals(
                OS.NEWLINE +
                        "@Article{," + OS.NEWLINE +
                        "  author = {A}," + OS.NEWLINE +
                        "  year   = {2010}," + OS.NEWLINE +
                        "}" + OS.NEWLINE + OS.NEWLINE +
                        "@Article{," + OS.NEWLINE +
                        "  author = {B}," + OS.NEWLINE +
                        "  year   = {2000}," + OS.NEWLINE +
                        "}" + OS.NEWLINE + OS.NEWLINE +
                        "@Article{," + OS.NEWLINE +
                        "  author = {A}," + OS.NEWLINE +
                        "  year   = {2000}," + OS.NEWLINE +
                        "}"
                        + OS.NEWLINE + OS.NEWLINE +
                        "@Comment{jabref-meta: databaseType:bibtex;}"
                        + OS.NEWLINE
                , session.getStringValue());
    }

    @Test
    public void roundtripWithContentSelectorsAndUmlauts() throws IOException, SaveException {
        String fileContent = "% Encoding: UTF-8" + OS.NEWLINE + OS.NEWLINE + "@Comment{jabref-meta: selector_journal:Test {\\\\\"U}mlaut;}" + OS.NEWLINE;
        Charset encoding = StandardCharsets.UTF_8;

        ParserResult firstParse = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(fileContent));

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        StringSaveSession session = databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries(), preferences);

        assertEquals(fileContent, session.getStringValue());
    }

}
