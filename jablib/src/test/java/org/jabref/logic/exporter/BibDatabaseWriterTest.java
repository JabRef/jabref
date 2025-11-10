package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import org.jabref.logic.os.OS;
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
class BibDatabaseWriterTest {

    private BibDatabaseWriter databaseWriter;
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
        fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = new BibEntryTypesManager();
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        initializeDatabaseWriter();
        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences()).thenReturn(fieldPreferences);
    }

    private void initializeDatabaseWriter() {
        databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
    }

    @Test
    void writeWithNullContextThrowsException() {
        assertThrows(NullPointerException.class, () -> databaseWriter.writePartOfDatabase(null, List.of()));
    }

    @Test
    void writeWithNullEntriesThrowsException() {
        assertThrows(NullPointerException.class, () -> databaseWriter.writePartOfDatabase(bibtexContext, null));
    }

    @Test
    void writeEncodingUsAsciiWhenSetInPreferencesAndHeader() throws IOException {
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEncodingWindows1252WhenSetInPreferencesAndHeader() throws IOException {
        metaData.setEncoding(Charset.forName("windows-1252"));

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("% Encoding: windows-1252" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreamble() throws IOException {
        database.setPreamble("Test preamble");

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@Preamble{Test preamble}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writePreambleAndEncoding() throws IOException {
        metaData.setEncoding(StandardCharsets.US_ASCII);
        database.setPreamble("Test preamble");

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@Preamble{Test preamble}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntry() throws IOException {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of(entry));

        assertEquals("@Article{," + OS.NEWLINE + "}"
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEntryWithDuplicateKeywords() throws IOException {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.KEYWORDS, "asdf,asdf,asdf");
        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of(entry));

        assertEquals("@Article{," + OS.NEWLINE
                        + "  keywords = {asdf,asdf,asdf}," + OS.NEWLINE
                        + "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void putKeyWordsRemovesDuplicateKeywordsIsVisibleDuringWrite() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.putKeywords(List.of("asdf", "asdf", "asdf"), ',');

        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of(entry));

        assertEquals("@Article{," + OS.NEWLINE
                        + "  keywords = {asdf}," + OS.NEWLINE
                        + "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEncodingAndEntry() throws IOException {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        database.insertEntry(entry);
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of(entry));

        assertEquals(
                "% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                        "@Article{," + OS.NEWLINE + "}"
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeEpilogue() throws IOException {
        database.setEpilog("Test epilog");

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("Test epilog" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEpilogueAndEncoding() throws IOException {
        database.setEpilog("Test epilog");
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "Test epilog" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void utf8EncodingWrittenIfExplicitlyDefined() throws IOException {
        metaData.setEncoding(StandardCharsets.UTF_8);
        metaData.setEncodingExplicitlySupplied(true);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("% Encoding: UTF-8" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void utf8EncodingNotWrittenIfNotExplicitlyDefined() throws IOException {
        metaData.setEncoding(StandardCharsets.UTF_8);
        metaData.setEncodingExplicitlySupplied(false);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("", stringWriter.toString());
    }

    @Test
    void writeMetadata() throws IOException {
        DatabaseCitationKeyPatterns bibtexKeyPattern = new DatabaseCitationKeyPatterns(mock(GlobalCitationKeyPatterns.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeMetadataAndEncoding() throws IOException {
        DatabaseCitationKeyPatterns bibtexKeyPattern = new DatabaseCitationKeyPatterns(mock(GlobalCitationKeyPatterns.class));
        bibtexKeyPattern.setDefaultValue("test");
        metaData.setCiteKeyPattern(bibtexKeyPattern);
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE
                +
                "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeGroups() throws IOException {
        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addSubgroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ','));
        metaData.setGroups(groupRoot);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        // @formatter:off
        assertEquals("@Comment{jabref-meta: grouping:" + OS.NEWLINE
                + "0 AllEntriesGroup:;" + OS.NEWLINE
                + "1 StaticGroup:test\\;2\\;1\\;\\;\\;\\;;" + OS.NEWLINE
                + "}" + OS.NEWLINE, stringWriter.toString());
        // @formatter:on
    }

    @Test
    void writeGroupsAndEncoding() throws IOException {
        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup(""));
        groupRoot.addChild(GroupTreeNode.fromGroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING, ',')));
        metaData.setGroups(groupRoot);
        metaData.setEncoding(StandardCharsets.US_ASCII);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

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
    void writeString() throws IOException {
        database.addString(new BibtexString("name", "content"));

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeStringWithQuotes() throws IOException {
        String parsedSerialization = "@String{name = \"content\"}";
        BibtexString bibtexString = new BibtexString("name", "content", parsedSerialization);
        database.addString(bibtexString);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals(parsedSerialization + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeStringAndEncoding() throws IOException {
        metaData.setEncoding(StandardCharsets.US_ASCII);
        database.addString(new BibtexString("name", "content"));

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("% Encoding: US-ASCII" + OS.NEWLINE + OS.NEWLINE +
                "@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void doNotWriteUtf8StringAndEncoding() throws IOException {
        database.addString(new BibtexString("name", "content"));

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntryWithCustomizedTypeAlsoWritesTypeDeclaration() throws IOException {
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

        databaseWriter.writeDatabase(bibtexContext);

        assertEquals("@Customizedtype{key," + OS.NEWLINE + "}" + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + OS.NEWLINE + OS.NEWLINE
                        + "@Comment{jabref-entrytype: customizedtype: req[title;author;date] opt[year;month;publisher]}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeCustomizedTypesInAlphabeticalOrder() throws IOException {
        EntryType customizedType = new UnknownEntryType("customizedType");
        EntryType otherCustomizedType = new UnknownEntryType("otherCustomizedType");
        BibEntryType customizedBibType = new BibEntryType(
                customizedType,
                List.of(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                List.of(new OrFields(StandardField.TITLE)));
        BibEntryType otherCustomizedBibType = new BibEntryType(
                otherCustomizedType,
                List.of(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                List.of(new OrFields(StandardField.TITLE)));
        entryTypesManager.addCustomOrModifiedType(otherCustomizedBibType, BibDatabaseMode.BIBTEX);
        entryTypesManager.addCustomOrModifiedType(customizedBibType, BibDatabaseMode.BIBTEX);
        BibEntry entry = new BibEntry(customizedType);
        BibEntry otherEntry = new BibEntry(otherCustomizedType);
        database.insertEntry(otherEntry);
        database.insertEntry(entry);
        bibtexContext.setMode(BibDatabaseMode.BIBTEX);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of(entry, otherEntry));

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
    void roundtripWithArticleMonths() throws IOException {
        Path testBibtexFile = Path.of("src/test/resources/testbib/articleWithMonths.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripUtf8EncodingHeaderRemoved() throws Exception {
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

        BibDatabaseContext context = BibDatabaseContext.of(bibtexEntry, importFormatPreferences);
        databaseWriter.writeDatabase(context);
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
    void roundtripWin1252HeaderKept(@TempDir Path bibFolder) throws IOException, URISyntaxException {
        Path testFile = Path.of(BibDatabaseWriterTest.class.getResource("encoding-windows-1252-with-header.bib").toURI());
        ParserResult result = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        Path pathToFile = bibFolder.resolve("JabRef.bib");
        Path file = Files.createFile(pathToFile);
        Charset charset = Charset.forName("windows-1252");

        try (BufferedWriter fileWriter = Files.newBufferedWriter(file, charset)) {
            BibWriter bibWriter = new BibWriter(fileWriter, context.getDatabase().getNewLineSeparator());
            BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    fieldPreferences,
                    citationKeyPatternPreferences,
                    entryTypesManager);
            databaseWriter.writeDatabase(context);
        }

        assertEquals(Files.readString(testFile, charset), Files.readString(file, charset));
    }

    @Test
    void roundtripUtf8HeaderKept(@TempDir Path bibFolder) throws URISyntaxException, IOException {
        Path testFile = Path.of(BibDatabaseWriterTest.class.getResource("encoding-utf-8-with-header-with-databasetypecomment.bib").toURI());
        ParserResult result = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        Path pathToFile = bibFolder.resolve("JabRef.bib");
        Path file = Files.createFile(pathToFile);
        Charset charset = StandardCharsets.UTF_8;

        try (BufferedWriter fileWriter = Files.newBufferedWriter(file, charset)) {
            BibWriter bibWriter = new BibWriter(fileWriter, context.getDatabase().getNewLineSeparator());
            BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    fieldPreferences,
                    citationKeyPatternPreferences,
                    entryTypesManager);
            databaseWriter.writeDatabase(context);
        }

        assertEquals(Files.readString(testFile, charset), Files.readString(file, charset));
    }

    @Test
    void roundtripNotExplicitUtf8HeaderNotInsertedDuringWrite(@TempDir Path bibFolder) throws URISyntaxException, IOException {
        Path testFile = Path.of(BibDatabaseWriterTest.class.getResource("encoding-utf-8-without-header-with-databasetypecomment.bib").toURI());
        ParserResult result = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(testFile);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        Path pathToFile = bibFolder.resolve("JabRef.bib");
        Path file = Files.createFile(pathToFile);
        Charset charset = StandardCharsets.UTF_8;

        try (BufferedWriter fileWriter = Files.newBufferedWriter(file, charset)) {
            BibWriter bibWriter = new BibWriter(fileWriter, context.getDatabase().getNewLineSeparator());
            BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                    bibWriter,
                    saveConfiguration,
                    fieldPreferences,
                    citationKeyPatternPreferences,
                    entryTypesManager);
            databaseWriter.writeDatabase(context);
        }

        assertEquals(Files.readString(testFile, charset), Files.readString(file, charset));
    }

    @Test
    void roundtripWithComplexBib() throws IOException {
        Path testBibtexFile = Path.of("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        BibWriter bibWriter = new BibWriter(stringWriter, context.getDatabase().getNewLineSeparator());
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUserComment() throws IOException {
        Path testBibtexFile = Path.of("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithOneUserCommentAndEntryChange() throws IOException {
        String bibEntry = "@Comment this in an unbracketed comment that should be preserved as well\n" +
                "\n" +
                "This is some arbitrary user comment that should be preserved\n" +
                "\n" +
                "@InProceedings{1137631,\n" +
                "  author     = {Mr. Author},\n" +
                "}\n";

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Reader.of(bibEntry));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // we need a new writer because "\n" instead of "OS.NEWLINE"
        bibWriter = new BibWriter(stringWriter, "\n");
        databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(bibEntry, stringWriter.toString());
    }

    @Test
    void roundtripWithTwoEntriesAndOneUserCommentAndEntryChange() throws IOException {
        String bibEntry = "@Article{test,}\n" +
                "\n" +
                "@Comment this in an unbracketed comment that should be preserved as well\n" +
                "\n" +
                "This is some arbitrary user comment that should be preserved\n" +
                "\n" +
                "@InProceedings{1137631,\n" +
                "  author     = {Mr. Author},\n" +
                "}\n";

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Reader.of(bibEntry));

        BibEntry entry = result.getDatabase().getEntryByCitationKey("1137631").get();
        entry.setField(StandardField.AUTHOR, "Mr. Author");

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // we need a new writer because "\n" instead of "OS.NEWLINE"
        bibWriter = new BibWriter(stringWriter, "\n");
        databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(bibEntry, stringWriter.toString());
    }

    @Test
    void roundtripWithUserCommentAndEntryChange() throws IOException {
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
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(Path.of("src/test/resources/testbib/bibWithUserCommentAndEntryChange.bib"), encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUserCommentBeforeStringAndChange() throws IOException {
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
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());

        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void roundtripWithUnknownMetaData() throws IOException {
        Path testBibtexFile = Path.of("src/test/resources/testbib/unknownMetaData.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = new BibtexParser(importFormatPreferences).parse(Importer.getReader(testBibtexFile));

        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        // .gitattributes sets the .bib files to have LF line endings
        // This needs to be reflected here
        bibWriter = new BibWriter(stringWriter, "\n");
        initializeDatabaseWriter();
        databaseWriter.writePartOfDatabase(context, result.getDatabase().getEntries());
        assertEquals(Files.readString(testBibtexFile, encoding), stringWriter.toString());
    }

    @Test
    void writeSavedSerializationOfEntryIfUnchanged() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Mr. author");
        entry.setParsedSerialization("presaved serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of(entry));

        assertEquals("presaved serialization" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void reformatEntryIfAskedToDoSo() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Mr. author");
        entry.setParsedSerialization("wrong serialization");
        entry.setChanged(false);
        database.insertEntry(entry);

        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, true);
        initializeDatabaseWriter();
        databaseWriter.writePartOfDatabase(bibtexContext, List.of(entry));

        assertEquals("@Article{," + OS.NEWLINE + "  author = {Mr. author}," + OS.NEWLINE + "}"
                        + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeSavedSerializationOfStringIfUnchanged() throws IOException {
        BibtexString string = new BibtexString("name", "content", "serialization");
        database.addString(string);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("serialization" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void reformatStringIfAskedToDoSo() throws IOException {
        BibtexString string = new BibtexString("name", "content", "wrong serialization");
        database.addString(string);

        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, true);
        initializeDatabaseWriter();
        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@String{name = {content}}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeSaveActions() throws IOException {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Arrays.asList(
                        new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter()),
                        new FieldFormatterCleanup(StandardField.JOURNAL, new TitleCaseFormatter()),
                        new FieldFormatterCleanup(StandardField.DAY, new UpperCaseFormatter())));
        metaData.setSaveActions(saveActions);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        // The order should be kept (the cleanups are a list, not a set)
        assertEquals("@Comment{jabref-meta: saveActions:enabled;"
                + OS.NEWLINE
                + "title[lower_case]" + OS.NEWLINE
                + "journal[title_case]" + OS.NEWLINE
                + "day[upper_case]" + OS.NEWLINE
                + ";}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeSaveOrderConfig() throws IOException {
        SaveOrder saveOrder = new SaveOrder(SaveOrder.OrderType.SPECIFIED,
                List.of(new SaveOrder.SortCriterion(StandardField.AUTHOR, false),
                        new SaveOrder.SortCriterion(StandardField.YEAR, true),
                        new SaveOrder.SortCriterion(StandardField.ABSTRACT, false)));
        metaData.setSaveOrder(saveOrder);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeCustomKeyPattern() throws IOException {
        AbstractCitationKeyPatterns pattern = new DatabaseCitationKeyPatterns(mock(GlobalCitationKeyPatterns.class));
        pattern.setDefaultValue("test");
        pattern.addCitationKeyPattern(StandardEntryType.Article, "articleTest");
        metaData.setCiteKeyPattern(pattern);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@Comment{jabref-meta: keypattern_article:articleTest;}" + OS.NEWLINE
                        + OS.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeBiblatexMode() throws IOException {
        metaData.setMode(BibDatabaseMode.BIBLATEX);

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@Comment{jabref-meta: databaseType:biblatex;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeProtectedFlag() throws IOException {
        metaData.markAsProtected();

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@Comment{jabref-meta: protectedFlag:true;}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void writeFileDirectories() throws IOException {
        metaData.setLibrarySpecificFileDirectory("\\Literature\\");
        metaData.setUserFileDirectory("defaultOwner-user", "D:\\Documents");
        metaData.setLatexFileDirectory("defaultOwner-user", "D:\\Latex");

        databaseWriter.writePartOfDatabase(bibtexContext, List.of());

        assertEquals("@Comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}" + OS.NEWLINE +
                OS.NEWLINE + "@Comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                + OS.NEWLINE + OS.NEWLINE + "@Comment{jabref-meta: fileDirectoryLatex-defaultOwner-user:D:\\\\Latex;}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntriesSorted() throws IOException {
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

        databaseWriter.writePartOfDatabase(bibtexContext, database.getEntries());

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
    void writeEntriesInOriginalOrderWhenNoSaveOrderConfigIsSetInMetadata() throws IOException {
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

        databaseWriter.writePartOfDatabase(bibtexContext, database.getEntries());

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
    void normalizeWhitespacesCleanupOnlyInTextFields() throws IOException {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Firstname1 Lastname1   and   Firstname2 Lastname2")
                .withField(StandardField.FILE, "some  --  filename  -- spaces.pdf")
                .withChanged(true);

        database.insertEntry(firstEntry);

        databaseWriter.writePartOfDatabase(bibtexContext, database.getEntries());

        assertEquals("""
                @Article{,
                  author = {Firstname1 Lastname1 and Firstname2 Lastname2},
                  file   = {some  --  filename  -- spaces.pdf},
                }
                """.replace("\n", OS.NEWLINE), stringWriter.toString());
    }

    @Test
    void trimFieldContents() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.NOTE, "        some note    \t")
                .withChanged(true);
        database.insertEntry(entry);

        databaseWriter.writeDatabase(bibtexContext);

        assertEquals("@Article{," + OS.NEWLINE +
                        "  note = {some note}," + OS.NEWLINE +
                        "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void newlineAtEndOfAbstractFieldIsDeleted() throws IOException {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.ABSTRACT, text + OS.NEWLINE);
        database.insertEntry(entry);

        databaseWriter.writeDatabase(bibtexContext);

        assertEquals("@Article{," + OS.NEWLINE +
                        "  abstract = {" + text + "}," + OS.NEWLINE +
                        "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void roundtripWithContentSelectorsAndUmlauts() throws IOException {
        String encodingHeader = "% Encoding: UTF-8" + OS.NEWLINE + OS.NEWLINE;
        String commentEntry = "@Comment{jabref-meta: selector_journal:Test {\\\\\"U}mlaut;}" + OS.NEWLINE;
        String fileContent = encodingHeader + commentEntry;
        Charset encoding = StandardCharsets.UTF_8;

        ParserResult firstParse = new BibtexParser(importFormatPreferences).parse(Reader.of(fileContent));

        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());

        databaseWriter.writePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals(commentEntry, stringWriter.toString());
    }

    @Test
    void saveAlsoSavesSecondModification() throws IOException {
        // @formatter:off
        String bibtexEntry = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "}";
        // @formatter:on

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult firstParse = new BibtexParser(importFormatPreferences).parse(Reader.of(bibtexEntry));
        Collection<BibEntry> entries = firstParse.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());
        context.setMode(BibDatabaseMode.BIBTEX);

        databaseWriter.writePartOfDatabase(context, firstParse.getDatabase().getEntries());

        // modify entry a second time
        entry.setField(StandardField.AUTHOR, "Test");

        // write a second time
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.writePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals("@Article{test," + OS.NEWLINE +
                "  author  = {Test}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE + OS.NEWLINE +
                "@Comment{jabref-meta: databaseType:bibtex;}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void saveReturnsToOriginalEntryWhenEntryIsFlaggedUnchanged() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult firstParse = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(Reader.of(bibtexEntry));
        Collection<BibEntry> entries = firstParse.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // flag unchanged
        entry.setChanged(false);

        // write entry
        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());
        databaseWriter.writePartOfDatabase(context, firstParse.getDatabase().getEntries());

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void saveReturnsToOriginalEntryWhenEntryIsFlaggedUnchangedEvenInThePresenceOfSavedModifications() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        ParserResult firstParse = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(Reader.of(bibtexEntry));
        Collection<BibEntry> entries = firstParse.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        BibDatabaseContext context = new BibDatabaseContext(firstParse.getDatabase(), firstParse.getMetaData());

        databaseWriter.writePartOfDatabase(context, firstParse.getDatabase().getEntries());

        // modify entry a second time
        entry.setField(StandardField.AUTHOR, "Test");

        entry.setChanged(false);

        // write a second time
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
        databaseWriter.writePartOfDatabase(context, firstParse.getDatabase().getEntries());

        // returns tu original entry, not to the last saved one
        assertEquals(bibtexEntry, stringWriter.toString());
    }
}
