package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPattern;
import org.jabref.logic.citationkeypattern.DatabaseCitationKeyPattern;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BibtexDatabaseWriterTest {

    private BibtexDatabaseWriter databaseWriter;
    private BibDatabase database;
    private MetaData metaData;
    private BibDatabaseContext bibtexContext;
    private ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();
    private GeneralPreferences generalPreferences;
    private SavePreferences savePreferences;
    private BibEntryTypesManager entryTypesManager;
    private StringWriter stringWriter;
    private BibWriter bibWriter;

    @BeforeEach
    void setUp() {
        generalPreferences = mock(GeneralPreferences.class);
        when(generalPreferences.getDefaultEncoding()).thenReturn(null);
        savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        entryTypesManager = new BibEntryTypesManager();
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        databaseWriter = new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, entryTypesManager);

        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData);
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
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreamble() throws Exception {
        database.setPreamble("Test preamble");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Preamble{Test preamble}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreambleAndEncoding() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);
        database.setPreamble("Test preamble");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@Preamble{Test preamble}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntry() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("@Article{," + OS.NEWLINE + "}"
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEncodingAndEntry() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals(
                "% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                        "@Article{," + OS.NEWLINE + "}"
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEpilogue() throws Exception {
        database.setEpilog("Test epilog");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("Test epilog" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEpilogueAndEncoding() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);
        database.setEpilog("Test epilog");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "Test epilog" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeMetadata() throws Exception {
        DatabaseCitationKeyPattern bibtexKeyPattern = new DatabaseCitationKeyPattern(mock(GlobalCitationKeyPattern.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeMetadataAndEncoding() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);
        DatabaseCitationKeyPattern bibtexKeyPattern = new DatabaseCitationKeyPattern(mock(GlobalCitationKeyPattern.class));
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
        assertEquals("@Comment{jabref-meta: grouping:" + OS.NEWLINE
                + "0 AllEntriesGroup:;" + OS.NEWLINE
                + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + OS.NEWLINE
                + "}" + OS.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    void writeGroupsAndEncoding() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);

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

        assertEquals("@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeStringAndEncoding() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.US_ASCII);
        database.addString(new BibtexString("name", "content"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void doNotWriteUtf8StringAndEncoding() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        database.addString(new BibtexString("name", "content"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntryWithCustomizedTypeAlsoWritesTypeDeclaration() throws Exception {
        EntryType customizedType = new UnknownEntryType("customizedType");
        BibEntryType customizedBibType = new BibEntryType(
                customizedType,
                Arrays.asList(
                        new BibField(StandardField.TITLE, FieldPriority.IMPORTANT),
                        new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT),
                        new BibField(StandardField.DATE, FieldPriority.IMPORTANT),
                        new BibField(StandardField.YEAR, FieldPriority.IMPORTANT),
                        new BibField(StandardField.MONTH, FieldPriority.IMPORTANT),
                        new BibField(StandardField.PUBLISHER, FieldPriority.IMPORTANT)),
                Arrays.asList(
                        new OrFields(StandardField.TITLE),
                        new OrFields(StandardField.AUTHOR),
                        new OrFields(StandardField.DATE)));
        entryTypesManager.addCustomOrModifiedType(customizedBibType, BibDatabaseMode.BIBTEX);
        BibEntry entry = new BibEntry(customizedType);
        database.insertEntry(entry);
        bibtexContext.setMode(BibDatabaseMode.BIBTEX);

        databaseWriter.saveDatabase(bibtexContext);

        assertEquals("@Customizedtype{," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-entrytype: customizedtype: req[title;author;date] opt[year;month;publisher]}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeCustomizedTypesInAlphabeticalOrder() throws Exception {
        EntryType customizedType = new UnknownEntryType("customizedType");
        EntryType otherCustomizedType = new UnknownEntryType("otherCustomizedType");
        BibEntryType customizedBibType = new BibEntryType(
                customizedType,
                Collections.singletonList(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Collections.singletonList(new OrFields(StandardField.TITLE)));
        BibEntryType otherCustomizedBibType = new BibEntryType(
                otherCustomizedType,
                Collections.singletonList(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Collections.singletonList(new OrFields(StandardField.TITLE)));
        entryTypesManager.addCustomOrModifiedType(otherCustomizedBibType, BibDatabaseMode.BIBTEX);
        entryTypesManager.addCustomOrModifiedType(customizedBibType, BibDatabaseMode.BIBTEX);
        BibEntry entry = new BibEntry(customizedType);
        BibEntry otherEntry = new BibEntry(otherCustomizedType);
        database.insertEntry(otherEntry);
        database.insertEntry(entry);
        bibtexContext.setMode(BibDatabaseMode.BIBTEX);

        databaseWriter.savePartOfDatabase(bibtexContext, Arrays.asList(entry, otherEntry));

        assertEquals(
                "@Customizedtype{," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                        + "@Othercustomizedtype{," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-entrytype: customizedtype: req[title] opt[]}" + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-entrytype: othercustomizedtype: req[title] opt[]}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void roundtripWithArticleMonths() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/articleWithMonths.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        when(generalPreferences.getDefaultEncoding()).thenReturn(encoding);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripUtf8EncodingHeaderRemoved() throws Exception {
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        // @formatter:off
        String bibtexEntry = OS.NEWLINE + "% Encoding: UTF8" + OS.NEWLINE +
                OS.NEWLINE +
                "@Article{," + OS.NEWLINE +
                "  author  = {Foo Bar}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());
        databaseWriter.saveDatabase(context);
        // @formatter:off
        String expected = "@Article{," + OS.NEWLINE +
                "  author  = {Foo Bar}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
       assertEquals(expected, stringWriter.toString());
    }

    @Test
    void roundtripWithComplexBib() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        when(generalPreferences.getDefaultEncoding()).thenReturn(encoding);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUserComment() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        when(generalPreferences.getDefaultEncoding()).thenReturn(encoding);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithOneUserCommentAndEntryChange() throws Exception {
        String bibEntry = "@Comment this in an unbracketed comment that should be preserved as well\n" +
                "\n" +
                "This is some arbitrary user comment that should be preserved\n" +
                "\n" +
                "@InProceedings{1137631,\n" +
                "  author     = {Mr. Author},\n" +
                "}\n";

        // read in bibtex string
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(new StringReader(bibEntry));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // we need a new writer because "\n" instead of "OS.NEWLINE"
        bibWriter = new BibWriter(stringWriter, "\n");
        databaseWriter = new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, entryTypesManager);
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(bibEntry, stringWriter.toString());
    }

    @Test
    void roundtripWithTwoEntriesAndOneUserCommentAndEntryChange() throws Exception {
        String bibEntry = "@Article{test,}\n" +
                "\n" +
                "@Comment this in an unbracketed comment that should be preserved as well\n" +
                "\n" +
                "This is some arbitrary user comment that should be preserved\n" +
                "\n" +
                "@InProceedings{1137631,\n" +
                "  author     = {Mr. Author},\n" +
                "}\n";

        // read in bibtex string
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(new StringReader(bibEntry));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // we need a new writer because "\n" instead of "OS.NEWLINE"
        bibWriter = new BibWriter(stringWriter, "\n");
        databaseWriter = new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, entryTypesManager);
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(bibEntry, stringWriter.toString());
    }

    @Test
    void roundtripWithUserCommentAndEntryChange() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        when(generalPreferences.getDefaultEncoding()).thenReturn(encoding);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(Path.of("src/test/resources/testbib/bibWithUserCommentAndEntryChange.bib"), encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUserCommentBeforeStringAndChange() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        for (BibtexString string : result.getDatabase().getStringValues()) {
            // Mark them as changed
            string.setContent(string.getContent());
        }

        when(generalPreferences.getDefaultEncoding()).thenReturn(encoding);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());

        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUnknownMetaData() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/unknownMetaData.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(Importer.getReader(testBibtexFile, encoding));

        when(generalPreferences.getDefaultEncoding()).thenReturn(encoding);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void writeSavedSerializationOfEntryIfUnchanged() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Mr. author");
        entry.setParsedSerialization("presaved serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("presaved serialization" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void reformatEntryIfAskedToDoSo() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Mr. author");
        entry.setParsedSerialization("wrong serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        when(savePreferences.shouldReformatFile()).thenReturn(true);
        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("@Article{," + OS.NEWLINE + "  author = {Mr. author}," + OS.NEWLINE + "}"
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeSavedSerializationOfStringIfUnchanged() throws Exception {
        BibtexString string = new BibtexString("name", "content");
        string.setParsedSerialization("serialization");
        database.addString(string);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("serialization" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void reformatStringIfAskedToDoSo() throws Exception {
        BibtexString string = new BibtexString("name", "content");
        string.setParsedSerialization("wrong serialization");
        database.addString(string);

        when(savePreferences.shouldReformatFile()).thenReturn(true);
        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeSaveActions() throws Exception {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Arrays.asList(
                        new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter()),
                        new FieldFormatterCleanup(StandardField.JOURNAL, new TitleCaseFormatter()),
                        new FieldFormatterCleanup(StandardField.DAY, new UpperCaseFormatter())));
        metaData.setSaveActions(saveActions);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: saveActions:enabled;"
                        + OS.NEWLINE
                        + "day[upper_case]" + OS.NEWLINE
                        + "journal[title_case]" + OS.NEWLINE
                        + "title[lower_case]" + OS.NEWLINE
                        + ";}"
                        + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeSaveOrderConfig() throws Exception {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(SaveOrderConfig.OrderType.SPECIFIED,
                List.of(new SaveOrderConfig.SortCriterion(StandardField.AUTHOR, false),
                        new SaveOrderConfig.SortCriterion(StandardField.YEAR, true),
                        new SaveOrderConfig.SortCriterion(StandardField.ABSTRACT, false)));
        metaData.setSaveOrderConfig(saveOrderConfig);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeCustomKeyPattern() throws Exception {
        AbstractCitationKeyPattern pattern = new DatabaseCitationKeyPattern(mock(GlobalCitationKeyPattern.class));
        pattern.setDefaultValue("test");
        pattern.addCitationKeyPattern(StandardEntryType.Article, "articleTest");
        metaData.setCiteKeyPattern(pattern);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: keypattern_article:articleTest;}" + OS.NEWLINE
                        + OS.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeBiblatexMode() throws Exception {
        metaData.setMode(BibDatabaseMode.BIBLATEX);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: databaseType:biblatex;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeProtectedFlag() throws Exception {
        metaData.markAsProtected();

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: protectedFlag:true;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeFileDirectories() throws Exception {
        metaData.setDefaultFileDirectory("\\Literature\\");
        metaData.setUserFileDirectory("defaultOwner-user", "D:\\Documents");
        metaData.setLatexFileDirectory("defaultOwner-user", Path.of("D:\\Latex"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}" + OS.NEWLINE +
                OS.NEWLINE + "@Comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                + OS.NEWLINE + OS.NEWLINE + "@Comment{jabref-meta: fileDirectoryLatex-defaultOwner-user:D:\\\\Latex;}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntriesSorted() throws Exception {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(SaveOrderConfig.OrderType.SPECIFIED,
                List.of(new SaveOrderConfig.SortCriterion(StandardField.AUTHOR, false),
                        new SaveOrderConfig.SortCriterion(StandardField.YEAR, true),
                        new SaveOrderConfig.SortCriterion(StandardField.ABSTRACT, false)));
        metaData.setSaveOrderConfig(saveOrderConfig);

        BibEntry firstEntry = new BibEntry();
        firstEntry.setType(StandardEntryType.Article);
        firstEntry.setField(StandardField.AUTHOR, "A");
        firstEntry.setField(StandardField.YEAR, "2010");

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType(StandardEntryType.Article);
        secondEntry.setField(StandardField.AUTHOR, "A");
        secondEntry.setField(StandardField.YEAR, "2000");

        BibEntry thirdEntry = new BibEntry();
        thirdEntry.setType(StandardEntryType.Article);
        thirdEntry.setField(StandardField.AUTHOR, "B");
        thirdEntry.setField(StandardField.YEAR, "2000");

        database.insertEntry(secondEntry);
        database.insertEntry(thirdEntry);
        database.insertEntry(firstEntry);

        databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries());

        assertEquals("@Article{," + OS.NEWLINE +
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
                        "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}" +
                        OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEntriesInOriginalOrderWhenNoSaveOrderConfigIsSetInMetadata() throws Exception {
        BibEntry firstEntry = new BibEntry();
        firstEntry.setType(StandardEntryType.Article);
        firstEntry.setField(StandardField.AUTHOR, "A");
        firstEntry.setField(StandardField.YEAR, "2010");

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType(StandardEntryType.Article);
        secondEntry.setField(StandardField.AUTHOR, "B");
        secondEntry.setField(StandardField.YEAR, "2000");

        BibEntry thirdEntry = new BibEntry();
        thirdEntry.setType(StandardEntryType.Article);
        thirdEntry.setField(StandardField.AUTHOR, "A");
        thirdEntry.setField(StandardField.YEAR, "2000");

        database.insertEntry(firstEntry);
        database.insertEntry(secondEntry);
        database.insertEntry(thirdEntry);

        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(false);
        databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries());

        assertEquals("@Article{," + OS.NEWLINE +
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
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void trimFieldContents() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.NOTE, "        some note    \t");
        database.insertEntry(entry);

        databaseWriter.saveDatabase(bibtexContext);

        assertEquals("@Article{," + OS.NEWLINE +
                        "  note = {some note}," + OS.NEWLINE +
                        "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void newlineAtEndOfAbstractFieldIsDeleted() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.ABSTRACT, text + OS.NEWLINE);
        database.insertEntry(entry);

        databaseWriter.saveDatabase(bibtexContext);

        assertEquals("@Article{," + OS.NEWLINE +
                        "  abstract = {" + text + "}," + OS.NEWLINE +
                        "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void roundtripWithContentSelectorsAndUmlauts() throws Exception {
        String encodingHeader = "% Encoding: UTF-8" + OS.NEWLINE + OS.NEWLINE;
        String commentEntry = "@Comment{jabref-meta: selector_journal:Test {\\\\\"U}mlaut;}" + OS.NEWLINE;
        String fileContent = encodingHeader + commentEntry;
        Charset encoding = StandardCharsets.UTF_8;

        ParserResult firstParse = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(fileContent));

        when(generalPreferences.getDefaultEncoding()).thenReturn(encoding);
        when(savePreferences.shouldSaveInOriginalOrder()).thenReturn(true);
        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());

        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals(commentEntry, stringWriter.toString());
    }

    @Test
    void saveAlsoSavesSecondModification() throws Exception {
        // @formatter:off
        String bibtexEntry = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult firstParse = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = firstParse.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());
        context.setMode(BibDatabaseMode.BIBTEX);

        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        // modify entry a second time
        entry.setField(StandardField.AUTHOR, "Test");

        // write a second time
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        databaseWriter = new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, entryTypesManager);
        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals("@Article{test," + OS.NEWLINE +
                "  author  = {Test}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE +
                "" + OS.NEWLINE +
                "@Comment{jabref-meta: databaseType:bibtex;}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void saveReturnsToOriginalEntryWhenEntryIsFlaggedUnchanged() throws Exception {
        // @formatter:off
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        // read in bibtex string
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult firstParse = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = firstParse.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // flag unchanged
        entry.setChanged(false);

        // write entry
        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());
        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void saveReturnsToOriginalEntryWhenEntryIsFlaggedUnchangedEvenInThePresenceOfSavedModifications() throws Exception {
        // @formatter:off
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        // read in bibtex string
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult firstParse = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = firstParse.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());

        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        // modify entry a second time
        entry.setField(StandardField.AUTHOR, "Test");

        entry.setChanged(false);

        // write a second time
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        databaseWriter = new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, entryTypesManager);
        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        // returns tu original entry, not to the last saved one
        assertEquals(bibtexEntry, stringWriter.toString());
    }
}
