package org.jabref.logic.exporter;

import java.io.BufferedWriter;
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

import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.AbstractCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.DatabaseCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.BibtexParser;
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
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for reading can be found at {@link org.jabref.logic.importer.fileformat.BibtexImporterTest}
 */
class BibtexDatabaseWriterTest {

    private BibtexDatabaseWriter databaseWriter;
    private BibDatabase database;
    private MetaData metaData;
    private BibDatabaseContext bibtexContext;
    private ImportFormatPreferences importFormatPreferences;
    private SelfContainedSaveConfiguration saveConfiguration;
    private FieldPreferences fieldPreferences;
    private CitationKeyPatternPreferences citationKeyPatternPreferences;
    private BibEntryTypesManager entryTypesManager;
    private StringWriter stringWriter;
    private BibWriter bibWriter;

    @BeforeEach
    void setUp() {
        fieldPreferences = new FieldPreferences(true, Collections.emptyList(), Collections.emptyList());
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = new BibEntryTypesManager();
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, NativeDesktop.NEWLINE);
        initializeDatabaseWriter();
        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences()).thenReturn(fieldPreferences);
    }

    private void initializeDatabaseWriter() {
        databaseWriter = new BibtexDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
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
    void writeEncodingUsAsciiWhenSetInPreferencesAndHeader() throws Exception {
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEncodingWindows1252WhenSetInPreferencesAndHeader() throws Exception {
        metaData.setEncoding(Charset.forName("windows-1252"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: windows-1252" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreamble() throws Exception {
        database.setPreamble("Test preamble");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Preamble{Test preamble}" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreambleAndEncoding() throws Exception {
        metaData.setEncoding(StandardCharsets.US_ASCII);
        database.setPreamble("Test preamble");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                "@Preamble{Test preamble}" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntry() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("@Article{," + NativeDesktop.NEWLINE + "}"
                        + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEntryWithDuplicateKeywords() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.KEYWORDS, "asdf,asdf,asdf");
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("@Article{," + NativeDesktop.NEWLINE
                        + "  keywords = {asdf,asdf,asdf}," + NativeDesktop.NEWLINE
                        + "}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void putKeyWordsRemovesDuplicateKeywordsIsVisibleDuringWrite() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.putKeywords(List.of("asdf", "asdf", "asdf"), ',');

        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("@Article{," + NativeDesktop.NEWLINE
                        + "  keywords = {asdf}," + NativeDesktop.NEWLINE
                        + "}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEncodingAndEntry() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        database.insertEntry(entry);
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals(
                "% Encoding: US-ASCII" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                        "@Article{," + NativeDesktop.NEWLINE + "}"
                        + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEpilogue() throws Exception {
        database.setEpilog("Test epilog");

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("Test epilog" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEpilogueAndEncoding() throws Exception {
        database.setEpilog("Test epilog");
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                "Test epilog" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void utf8EncodingWrittenIfExplicitlyDefined() throws Exception {
        metaData.setEncoding(StandardCharsets.UTF_8);
        metaData.setEncodingExplicitlySupplied(true);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: UTF-8" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void utf8EncodingNotWrittenIfNotExplicitlyDefined() throws Exception {
        metaData.setEncoding(StandardCharsets.UTF_8);
        metaData.setEncodingExplicitlySupplied(false);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("", stringWriter.toString());
    }

    @Test
    void writeMetadata() throws Exception {
        DatabaseCitationKeyPatterns bibtexKeyPattern = new DatabaseCitationKeyPatterns(mock(GlobalCitationKeyPatterns.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: keypatterndefault:test;}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeMetadataAndEncoding() throws Exception {
        DatabaseCitationKeyPatterns bibtexKeyPattern = new DatabaseCitationKeyPatterns(mock(GlobalCitationKeyPatterns.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE
                +
                "@Comment{jabref-meta: keypatterndefault:test;}" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeGroups() throws Exception {
        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addSubgroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ','));
        metaData.setGroups(groupRoot);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        // @formatter:off
        assertEquals("@Comment{jabref-meta: grouping:" + NativeDesktop.NEWLINE
                + "0 AllEntriesGroup:;" + NativeDesktop.NEWLINE
                + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + NativeDesktop.NEWLINE
                + "}" + NativeDesktop.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    void writeGroupsAndEncoding() throws Exception {
        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addChild(GroupTreeNode.fromGroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ',')));
        metaData.setGroups(groupRoot);
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        // @formatter:off
        assertEquals(
                "% Encoding: US-ASCII" + NativeDesktop.NEWLINE +
                        NativeDesktop.NEWLINE
                        + "@Comment{jabref-meta: grouping:" + NativeDesktop.NEWLINE
                        + "0 AllEntriesGroup:;" + NativeDesktop.NEWLINE
                        + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + NativeDesktop.NEWLINE
                        + "}" + NativeDesktop.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    void writeString() throws Exception {
        database.addString(new BibtexString("name", "content"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@String{name = {content}}" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeStringWithQuotes() throws Exception {
        String parsedSerialization = "@String{name = \"content\"}";
        BibtexString bibtexString = new BibtexString("name", "content", parsedSerialization);
        database.addString(bibtexString);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals(parsedSerialization + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeStringAndEncoding() throws Exception {
        metaData.setEncoding(StandardCharsets.US_ASCII);
        database.addString(new BibtexString("name", "content"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("% Encoding: US-ASCII" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                "@String{name = {content}}" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void doNotWriteUtf8StringAndEncoding() throws Exception {
        database.addString(new BibtexString("name", "content"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@String{name = {content}}" + NativeDesktop.NEWLINE, stringWriter.toString());
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
        BibEntry entry = new BibEntry(customizedType).withCitationKey("key");
        // needed to get a proper serialization
        entry.setChanged(true);
        database.insertEntry(entry);
        bibtexContext.setMode(BibDatabaseMode.BIBTEX);

        databaseWriter.saveDatabase(bibtexContext);

        assertEquals("@Customizedtype{key," + NativeDesktop.NEWLINE + "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE
                        + "@Comment{jabref-entrytype: customizedtype: req[title;author;date] opt[year;month;publisher]}" + NativeDesktop.NEWLINE,
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

        databaseWriter.savePartOfDatabase(bibtexContext, List.of(entry, otherEntry));

        assertEquals(
                "@Customizedtype{," + NativeDesktop.NEWLINE + "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE
                        + "@Othercustomizedtype{," + NativeDesktop.NEWLINE + "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE
                        + "@Comment{jabref-entrytype: customizedtype: req[title] opt[]}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE
                        + "@Comment{jabref-entrytype: othercustomizedtype: req[title] opt[]}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void roundtripWithArticleMonths() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/articleWithMonths.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripUtf8EncodingHeaderRemoved() throws Exception {
        // @formatter:off
        String bibtexEntry = NativeDesktop.NEWLINE + "% Encoding: UTF8" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                "@Article{," + NativeDesktop.NEWLINE +
                "  author  = {Foo Bar}," + NativeDesktop.NEWLINE +
                "  journal = {International Journal of Something}," + NativeDesktop.NEWLINE +
                "  note    = {some note}," + NativeDesktop.NEWLINE +
                "  number  = {1}," + NativeDesktop.NEWLINE +
                "}" + NativeDesktop.NEWLINE;
        // @formatter:on
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());
        databaseWriter.saveDatabase(context);
        // @formatter:off
        String expected = "@Article{," + NativeDesktop.NEWLINE +
                "  author  = {Foo Bar}," + NativeDesktop.NEWLINE +
                "  journal = {International Journal of Something}," + NativeDesktop.NEWLINE +
                "  note    = {some note}," + NativeDesktop.NEWLINE +
                "  number  = {1}," + NativeDesktop.NEWLINE +
                "}" + NativeDesktop.NEWLINE;
        // @formatter:on
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void roundtripWin1252HeaderKept(@TempDir Path bibFolder) throws Exception {
        Path testFile = Path.of(BibtexDatabaseWriterTest.class.getResource("encoding-windows-1252-with-header.bib").toURI());
        ParserResult result = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        Path pathToFile = bibFolder.resolve("JabRef.bib");
        Path file = Files.createFile(pathToFile);
        Charset charset = Charset.forName("windows-1252");

        try (BufferedWriter fileWriter = Files.newBufferedWriter(file, charset)) {
            BibWriter bibWriter = new BibWriter(fileWriter, context.getDatabase().getNewLineSeparator());
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    fieldPreferences,
                    citationKeyPatternPreferences,
                    entryTypesManager);
            databaseWriter.saveDatabase(context);
        }

        assertEquals(Files.readString(testFile, charset), Files.readString(file, charset));
    }

    @Test
    void roundtripUtf8HeaderKept(@TempDir Path bibFolder) throws Exception {
        Path testFile = Path.of(BibtexDatabaseWriterTest.class.getResource("encoding-utf-8-with-header-with-databasetypecomment.bib").toURI());
        ParserResult result = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        Path pathToFile = bibFolder.resolve("JabRef.bib");
        Path file = Files.createFile(pathToFile);
        Charset charset = StandardCharsets.UTF_8;

        try (BufferedWriter fileWriter = Files.newBufferedWriter(file, charset)) {
            BibWriter bibWriter = new BibWriter(fileWriter, context.getDatabase().getNewLineSeparator());
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    fieldPreferences,
                    citationKeyPatternPreferences,
                    entryTypesManager);
            databaseWriter.saveDatabase(context);
        }

        assertEquals(Files.readString(testFile, charset), Files.readString(file, charset));
    }

    @Test
    void roundtripNotExplicitUtf8HeaderNotInsertedDuringWrite(@TempDir Path bibFolder) throws Exception {
        Path testFile = Path.of(BibtexDatabaseWriterTest.class.getResource("encoding-utf-8-without-header-with-databasetypecomment.bib").toURI());
        ParserResult result = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        Path pathToFile = bibFolder.resolve("JabRef.bib");
        Path file = Files.createFile(pathToFile);
        Charset charset = StandardCharsets.UTF_8;

        try (BufferedWriter fileWriter = Files.newBufferedWriter(file, charset)) {
            BibWriter bibWriter = new BibWriter(fileWriter, context.getDatabase().getNewLineSeparator());
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    fieldPreferences,
                    citationKeyPatternPreferences,
                    entryTypesManager);
            databaseWriter.saveDatabase(context);
        }

        assertEquals(Files.readString(testFile, charset), Files.readString(file, charset));
    }

    @Test
    void roundtripWithComplexBib() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        BibWriter bibWriter = new BibWriter(stringWriter, context.getDatabase().getNewLineSeparator());
        BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUserComment() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
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
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibEntry));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // we need a new writer because "\n" instead of "OS.NEWLINE"
        bibWriter = new BibWriter(stringWriter, "\n");
        databaseWriter = new BibtexDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
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
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibEntry));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // we need a new writer because "\n" instead of "OS.NEWLINE"
        bibWriter = new BibWriter(stringWriter, "\n");
        databaseWriter = new BibtexDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(bibEntry, stringWriter.toString());
    }

    @Test
    void roundtripWithUserCommentAndEntryChange() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(Path.of("src/test/resources/testbib/bibWithUserCommentAndEntryChange.bib"), encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUserCommentBeforeStringAndChange() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        for (BibtexString string : result.getDatabase().getStringValues()) {
            // Mark them as changed
            string.setContent(string.getContent());
        }

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());

        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUnknownMetaData() throws Exception {
        Path testBibtexFile = Path.of("src/test/resources/testbib/unknownMetaData.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
        databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void writeSavedSerializationOfEntryIfUnchanged() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Mr. author");
        entry.setParsedSerialization("presaved serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("presaved serialization" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void reformatEntryIfAskedToDoSo() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Mr. author");
        entry.setParsedSerialization("wrong serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, true);
        initializeDatabaseWriter();
        databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry));

        assertEquals("@Article{," + NativeDesktop.NEWLINE + "  author = {Mr. author}," + NativeDesktop.NEWLINE + "}"
                        + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeSavedSerializationOfStringIfUnchanged() throws Exception {
        BibtexString string = new BibtexString("name", "content", "serialization");
        database.addString(string);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("serialization" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void reformatStringIfAskedToDoSo() throws Exception {
        BibtexString string = new BibtexString("name", "content", "wrong serialization");
        database.addString(string);

        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, true);
        initializeDatabaseWriter();
        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@String{name = {content}}" + NativeDesktop.NEWLINE, stringWriter.toString());
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

        // The order should be kept (the cleanups are a list, not a set)
        assertEquals("@Comment{jabref-meta: saveActions:enabled;"
                + NativeDesktop.NEWLINE
                + "title[lower_case]" + NativeDesktop.NEWLINE
                + "journal[title_case]" + NativeDesktop.NEWLINE
                + "day[upper_case]" + NativeDesktop.NEWLINE
                + ";}"
                + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeSaveOrderConfig() throws Exception {
        SaveOrder saveOrder = new SaveOrder(SaveOrder.OrderType.SPECIFIED,
                List.of(new SaveOrder.SortCriterion(StandardField.AUTHOR, false),
                        new SaveOrder.SortCriterion(StandardField.YEAR, true),
                        new SaveOrder.SortCriterion(StandardField.ABSTRACT, false)));
        metaData.setSaveOrder(saveOrder);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"
                + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeCustomKeyPattern() throws Exception {
        AbstractCitationKeyPatterns pattern = new DatabaseCitationKeyPatterns(mock(GlobalCitationKeyPatterns.class));
        pattern.setDefaultValue("test");
        pattern.addCitationKeyPattern(StandardEntryType.Article, "articleTest");
        metaData.setCiteKeyPattern(pattern);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: keypattern_article:articleTest;}" + NativeDesktop.NEWLINE
                        + NativeDesktop.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeBiblatexMode() throws Exception {
        metaData.setMode(BibDatabaseMode.BIBLATEX);

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: databaseType:biblatex;}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeProtectedFlag() throws Exception {
        metaData.markAsProtected();

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: protectedFlag:true;}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeFileDirectories() throws Exception {
        metaData.setDefaultFileDirectory("\\Literature\\");
        metaData.setUserFileDirectory("defaultOwner-user", "D:\\Documents");
        metaData.setLatexFileDirectory("defaultOwner-user", Path.of("D:\\Latex"));

        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList());

        assertEquals("@Comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE + "@Comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE + "@Comment{jabref-meta: fileDirectoryLatex-defaultOwner-user:D:\\\\Latex;}" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntriesSorted() throws Exception {
        SaveOrder saveOrder = new SaveOrder(SaveOrder.OrderType.SPECIFIED,
                List.of(new SaveOrder.SortCriterion(StandardField.AUTHOR, false),
                        new SaveOrder.SortCriterion(StandardField.YEAR, true),
                        new SaveOrder.SortCriterion(StandardField.ABSTRACT, false)));
        metaData.setSaveOrder(saveOrder);

        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "A")
                .withField(StandardField.YEAR, "2010")
                .withChanged(true);

        BibEntry secondEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "A")
                .withField(StandardField.YEAR, "2000")
                .withChanged(true);

        BibEntry thirdEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "B")
                .withField(StandardField.YEAR, "2000")
                .withChanged(true);

        database.insertEntries(secondEntry, thirdEntry, firstEntry);

        databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries());

        assertEquals("@Article{," + NativeDesktop.NEWLINE +
                        "  author = {A}," + NativeDesktop.NEWLINE +
                        "  year   = {2010}," + NativeDesktop.NEWLINE +
                        "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                        "@Article{," + NativeDesktop.NEWLINE +
                        "  author = {A}," + NativeDesktop.NEWLINE +
                        "  year   = {2000}," + NativeDesktop.NEWLINE +
                        "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                        "@Article{," + NativeDesktop.NEWLINE +
                        "  author = {B}," + NativeDesktop.NEWLINE +
                        "  year   = {2000}," + NativeDesktop.NEWLINE +
                        "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                        "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}" +
                        NativeDesktop.NEWLINE,
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

        databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries());

        assertEquals("@Article{," + NativeDesktop.NEWLINE +
                        "  author = {A}," + NativeDesktop.NEWLINE +
                        "  year   = {2010}," + NativeDesktop.NEWLINE +
                        "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                        "@Article{," + NativeDesktop.NEWLINE +
                        "  author = {B}," + NativeDesktop.NEWLINE +
                        "  year   = {2000}," + NativeDesktop.NEWLINE +
                        "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                        "@Article{," + NativeDesktop.NEWLINE +
                        "  author = {A}," + NativeDesktop.NEWLINE +
                        "  year   = {2000}," + NativeDesktop.NEWLINE +
                        "}"
                        + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void normalizeWhitespacesCleanupOnlyInTextFields() throws Exception {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Firstname1 Lastname1   and   Firstname2 Lastname2")
                .withField(StandardField.FILE, "some  --  filename  -- spaces.pdf")
                .withChanged(true);

        database.insertEntry(firstEntry);

        databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries());

        assertEquals("""
                @Article{,
                  author = {Firstname1 Lastname1 and Firstname2 Lastname2},
                  file   = {some  --  filename  -- spaces.pdf},
                }
                """.replace("\n", NativeDesktop.NEWLINE), stringWriter.toString());
    }

    @Test
    void trimFieldContents() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.NOTE, "        some note    \t")
                .withChanged(true);
        database.insertEntry(entry);

        databaseWriter.saveDatabase(bibtexContext);

        assertEquals("@Article{," + NativeDesktop.NEWLINE +
                        "  note = {some note}," + NativeDesktop.NEWLINE +
                        "}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void newlineAtEndOfAbstractFieldIsDeleted() throws Exception {
        String text = "lorem ipsum lorem ipsum" + NativeDesktop.NEWLINE + "lorem ipsum lorem ipsum";

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.ABSTRACT, text + NativeDesktop.NEWLINE);
        database.insertEntry(entry);

        databaseWriter.saveDatabase(bibtexContext);

        assertEquals("@Article{," + NativeDesktop.NEWLINE +
                        "  abstract = {" + text + "}," + NativeDesktop.NEWLINE +
                        "}" + NativeDesktop.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void roundtripWithContentSelectorsAndUmlauts() throws Exception {
        String encodingHeader = "% Encoding: UTF-8" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE;
        String commentEntry = "@Comment{jabref-meta: selector_journal:Test {\\\\\"U}mlaut;}" + NativeDesktop.NEWLINE;
        String fileContent = encodingHeader + commentEntry;
        Charset encoding = StandardCharsets.UTF_8;

        ParserResult firstParse = new BibtexParser(importFormatPreferences).parse(new StringReader(fileContent));

        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());

        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals(commentEntry, stringWriter.toString());
    }

    @Test
    void saveAlsoSavesSecondModification() throws Exception {
        // @formatter:off
        String bibtexEntry = NativeDesktop.NEWLINE + "@Article{test," + NativeDesktop.NEWLINE +
                "  Author                   = {Foo Bar}," + NativeDesktop.NEWLINE +
                "  Journal                  = {International Journal of Something}," + NativeDesktop.NEWLINE +
                "  Note                     = {some note}," + NativeDesktop.NEWLINE +
                "  Number                   = {1}," + NativeDesktop.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult firstParse = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
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
        bibWriter = new BibWriter(stringWriter, NativeDesktop.NEWLINE);
        databaseWriter = new BibtexDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals("@Article{test," + NativeDesktop.NEWLINE +
                "  author  = {Test}," + NativeDesktop.NEWLINE +
                "  journal = {International Journal of Something}," + NativeDesktop.NEWLINE +
                "  note    = {some note}," + NativeDesktop.NEWLINE +
                "  number  = {1}," + NativeDesktop.NEWLINE +
                "}" + NativeDesktop.NEWLINE + NativeDesktop.NEWLINE +
                "@Comment{jabref-meta: databaseType:bibtex;}" + NativeDesktop.NEWLINE, stringWriter.toString());
    }

    @Test
    void saveReturnsToOriginalEntryWhenEntryIsFlaggedUnchanged() throws Exception {
        // @formatter:off
        String bibtexEntry = "@Article{test," + NativeDesktop.NEWLINE +
                "  Author                   = {Foo Bar}," + NativeDesktop.NEWLINE +
                "  Journal                  = {International Journal of Something}," + NativeDesktop.NEWLINE +
                "  Number                   = {1}," + NativeDesktop.NEWLINE +
                "  Note                     = {some note}," + NativeDesktop.NEWLINE +
                "}" + NativeDesktop.NEWLINE;
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
        String bibtexEntry = "@Article{test," + NativeDesktop.NEWLINE +
                "  Author                   = {Foo Bar}," + NativeDesktop.NEWLINE +
                "  Journal                  = {International Journal of Something}," + NativeDesktop.NEWLINE +
                "  Note                     = {some note}," + NativeDesktop.NEWLINE +
                "  Number                   = {1}," + NativeDesktop.NEWLINE +
                "}" + NativeDesktop.NEWLINE;
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
        bibWriter = new BibWriter(stringWriter, NativeDesktop.NEWLINE);
        databaseWriter = new BibtexDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.savePartOfDatabase(context, firstParse.getDatabase().getEntries());

        // returns tu original entry, not to the last saved one
        assertEquals(bibtexEntry, stringWriter.toString());
    }
}
