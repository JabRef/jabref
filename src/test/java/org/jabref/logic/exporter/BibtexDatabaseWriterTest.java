package org.jabref.logic.exporter;

import java.io.StringReader;
import java.io.StringWriter;
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
import static org.mockito.Mockito.when;

class BibtexDatabaseWriterTest {

    private StringWriter stringWriter;
    private BibtexDatabaseWriter databaseWriter;
    private BibDatabase database;
    private MetaData metaData;
    private BibDatabaseContext bibtexContext;
    private ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();
    private SavePreferences preferences;

    @BeforeEach
    void setUp() {
        stringWriter = new StringWriter();
        preferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(preferences.getEncoding()).thenReturn(null);
        when(preferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        databaseWriter = new BibtexDatabaseWriter(stringWriter, preferences);

        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData, new Defaults(BibDatabaseMode.BIBTEX));
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    void writeWithNullContextThrowsException() throws Exception {
        assertThrows(NullPointerException.class, () -> databaseWriter.savePartOfDatabase(null, Collections.emptyList()));
    }

    @Test
    void writeWithNullEntriesThrowsException() throws Exception {
        assertThrows(NullPointerException.class, () -> databaseWriter.savePartOfDatabase(bibtexContext, null));
    }

    @Test
    void writeEncoding() throws Exception {
        when(preferences.getEncoding()).thenReturn(StandardCharsets.US_ASCII);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreamble() throws Exception {
        database.setPreamble("Test preamble");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@Preamble{Test preamble}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreambleAndEncoding() throws Exception {
        when(preferences.getEncoding()).thenReturn(StandardCharsets.US_ASCII);
        database.setPreamble("Test preamble");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@Preamble{Test preamble}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntry() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals(OS.NEWLINE +
                "@Article{," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEncodingAndEntry() throws Exception {
        when(preferences.getEncoding()).thenReturn(StandardCharsets.US_ASCII);
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@Article{," + OS.NEWLINE + "}"
                + OS.NEWLINE + OS.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEpilogue() throws Exception {
        database.setEpilog("Test epilog");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "Test epilog" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEpilogueAndEncoding() throws Exception {
        when(preferences.getEncoding()).thenReturn(StandardCharsets.US_ASCII);
        database.setEpilog("Test epilog");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "Test epilog" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeMetadata() throws Exception {
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(mock(GlobalBibtexKeyPattern.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeMetadataAndEncoding() throws Exception {
        when(preferences.getEncoding()).thenReturn(StandardCharsets.US_ASCII);
        DatabaseBibtexKeyPattern bibtexKeyPattern = new DatabaseBibtexKeyPattern(mock(GlobalBibtexKeyPattern.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE
                +
                "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeGroups() throws Exception {
        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addSubgroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ','));
        metaData.setGroups(groupRoot);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        // @formatter:off
        assertEquals(OS.NEWLINE
                + "@Comment{jabref-meta: grouping:" + OS.NEWLINE
                + "0 AllEntriesGroup:;" + OS.NEWLINE
                + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + OS.NEWLINE
                + "}" + OS.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    void writeGroupsAndEncoding() throws Exception {
        when(preferences.getEncoding()).thenReturn(StandardCharsets.US_ASCII);

        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addChild(GroupTreeNode.fromGroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ',')));
        metaData.setGroups(groupRoot);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        // @formatter:off
        assertEquals(
                "% Encoding: US-ASCII" + OS.NEWLINE +
                        OS.NEWLINE
                        + "@Comment{jabref-meta: grouping:" + OS.NEWLINE
                        + "0 AllEntriesGroup:;" + OS.NEWLINE
                        + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + OS.NEWLINE
                        + "}" + OS.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    void writeString() throws Exception {
        database.addString(new BibtexString("name", "content"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeStringAndEncoding() throws Exception {
        when(preferences.getEncoding()).thenReturn(StandardCharsets.US_ASCII);
        database.addString(new BibtexString("name", "content"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntryWithCustomizedTypeAlsoWritesTypeDeclaration() throws Exception {
        try {
            EntryTypes.addOrModifyCustomEntryType(new CustomEntryType("customizedType", "required", "optional"), BibDatabaseMode.BIBTEX);
            BibEntry entry = new BibEntry();
            entry.setType("customizedType");
            database.insertEntry(entry);

            databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

            assertEquals(
                    OS.NEWLINE +
                            "@Customizedtype{," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                            + "@Comment{jabref-meta: databaseType:bibtex;}"
                            + OS.NEWLINE + OS.NEWLINE
                            + "@Comment{jabref-entrytype: Customizedtype: req[required] opt[optional]}" + OS.NEWLINE,
                    stringWriter.toString());
        } finally {
            EntryTypes.removeAllCustomEntryTypes();
        }
    }

    @Test
    void roundtrip() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        when(preferences.getEncoding()).thenReturn(encoding);
        when(preferences.isSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        try (Scanner scanner = new Scanner(testBibtexFile, encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    void roundtripWithUserComment() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        when(preferences.getEncoding()).thenReturn(encoding);
        when(preferences.isSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        try (Scanner scanner = new Scanner(testBibtexFile, encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    void roundtripWithUserCommentAndEntryChange() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        BibEntry entry = result.getDatabase().getEntryByKey("1137631").get();
        entry.setField("author", "Mr. Author");

        when(preferences.getEncoding()).thenReturn(encoding);
        when(preferences.isSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());

        try (Scanner scanner = new Scanner(Paths.get("src/test/resources/testbib/bibWithUserCommentAndEntryChange.bib"), encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    void roundtripWithUserCommentBeforeStringAndChange() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        for (BibtexString string : result.getDatabase().getStringValues()) {
            // Mark them as changed
            string.setContent(string.getContent());
        }

        when(preferences.getEncoding()).thenReturn(encoding);
        when(preferences.isSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());

        try (Scanner scanner = new Scanner(testBibtexFile, encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    void roundtripWithUnknownMetaData() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/unknownMetaData.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        when(preferences.getEncoding()).thenReturn(encoding);
        when(preferences.isSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        try (Scanner scanner = new Scanner(testBibtexFile, encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    void writeSavedSerializationOfEntryIfUnchanged() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("presaved serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("presaved serialization" + OS.NEWLINE + "@Comment{jabref-meta: databaseType:bibtex;}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void reformatEntryIfAskedToDoSo() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "Mr. author");
        entry.setParsedSerialization("wrong serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        when(preferences.isReformatFile()).thenReturn(true);
        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals(OS.NEWLINE +
                        "@Article{," + OS.NEWLINE + "  author = {Mr. author}," + OS.NEWLINE + "}"
                        + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeSavedSerializationOfStringIfUnchanged() throws Exception {
        BibtexString string = new BibtexString("name", "content");
        string.setParsedSerialization("serialization");
        database.addString(string);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("serialization", stringWriter.toString());
    }

    @Test
    void reformatStringIfAskedToDoSo() throws Exception {
        BibtexString string = new BibtexString("name", "content");
        string.setParsedSerialization("wrong serialization");
        database.addString(string);

        when(preferences.isReformatFile()).thenReturn(true);
        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeSaveActions() throws Exception {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: saveActions:enabled;" + OS.NEWLINE
                + "title[lower_case]" + OS.NEWLINE + ";}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeSaveOrderConfig() throws Exception {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(false, true, new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true),
                new SaveOrderConfig.SortCriterion("abstract", false));
        metaData.setSaveOrderConfig(saveOrderConfig);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE
                + "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeCustomKeyPattern() throws Exception {
        AbstractBibtexKeyPattern pattern = new DatabaseBibtexKeyPattern(mock(GlobalBibtexKeyPattern.class));
        pattern.setDefaultValue("test");
        pattern.addBibtexKeyPattern("article", "articleTest");
        metaData.setCiteKeyPattern(pattern);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: keypattern_article:articleTest;}" + OS.NEWLINE
                        + OS.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeBiblatexMode() throws Exception {
        metaData.setMode(BibDatabaseMode.BIBLATEX);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: databaseType:biblatex;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeProtectedFlag() throws Exception {
        metaData.markAsProtected();

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: protectedFlag:true;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeFileDirectories() throws Exception {
        metaData.setDefaultFileDirectory("\\Literature\\");
        metaData.setUserFileDirectory("defaultOwner-user", "D:\\Documents");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(OS.NEWLINE + "@Comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}" + OS.NEWLINE +
                OS.NEWLINE + "@Comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntriesSorted() throws Exception {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(false, true,
                new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true),
                new SaveOrderConfig.SortCriterion("abstract", false));
        metaData.setSaveOrderConfig(saveOrderConfig);

        BibEntry firstEntry = new BibEntry();
        firstEntry.setType(BibtexEntryTypes.ARTICLE);
        firstEntry.setField("author", "A");
        firstEntry.setField("year", "2010");

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType(BibtexEntryTypes.ARTICLE);
        secondEntry.setField("author", "A");
        secondEntry.setField("year", "2000");

        BibEntry thirdEntry = new BibEntry();
        thirdEntry.setType(BibtexEntryTypes.ARTICLE);
        thirdEntry.setField("author", "B");
        thirdEntry.setField("year", "2000");

        database.insertEntry(secondEntry);
        database.insertEntry(thirdEntry);
        database.insertEntry(firstEntry);

        databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries());

        assertEquals(
                OS.NEWLINE +
                        "@Article{," + OS.NEWLINE +
                        "  author = {A}," + OS.NEWLINE +
                        "  year   = {2010}," + OS.NEWLINE +
                        "}" + OS.NEWLINE + OS.NEWLINE +
                        "@Article{," + OS.NEWLINE +
                        "  author = {A}," + OS.NEWLINE +
                        "  year   = {2000}," + OS.NEWLINE +
                        "}" + OS.NEWLINE + OS.NEWLINE +
                        "@Article{," + OS.NEWLINE +
                        "  author = {B}," + OS.NEWLINE +
                        "  year   = {2000}," + OS.NEWLINE +
                        "}" + OS.NEWLINE + OS.NEWLINE +
                        "@Comment{jabref-meta: databaseType:bibtex;}"
                        + OS.NEWLINE + OS.NEWLINE +
                        "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}" +
                        OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEntriesInOriginalOrderWhenNoSaveOrderConfigIsSetInMetadata() throws Exception {
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

        when(preferences.isSaveInOriginalOrder()).thenReturn(false);
        databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries());

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
                , stringWriter.toString());
    }

    @Test
    void roundtripWithContentSelectorsAndUmlauts() throws Exception {
        String fileContent = "% Encoding: UTF-8" + OS.NEWLINE + OS.NEWLINE + "@Comment{jabref-meta: selector_journal:Test {\\\\\"U}mlaut;}" + OS.NEWLINE;
        Charset encoding = StandardCharsets.UTF_8;

        ParserResult firstParse = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(fileContent));

        when(preferences.getEncoding()).thenReturn(encoding);
        when(preferences.isSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals(fileContent, stringWriter.toString());
    }
}
