package net.sf.jabref.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.fileformat.ImportFormat;
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BibtexDatabaseWriterTest {

    private BibtexDatabaseWriter<StringSaveSession> databaseWriter;
    private BibDatabase database;
    private MetaData metaData;
    private BibDatabaseContext bibtexContext;

    @Before
    public void setUp() throws SaveException, IOException {
        Globals.prefs = JabRefPreferences.getInstance();

        // Write to a string instead of to a file
        databaseWriter = new BibtexDatabaseWriter<>(StringSaveSession::new);

        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData, new Defaults(BibDatabaseMode.BIBTEX));
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullContextThrowsException() throws Exception {
        databaseWriter.savePartOfDatabase(null, Collections.emptyList(), new SavePreferences());
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullEntriesThrowsException() throws Exception {
        databaseWriter.savePartOfDatabase(bibtexContext, null, new SavePreferences());
    }

    @Test(expected = NullPointerException.class)
    public void writeWithNullPreferencesThrowsException() throws Exception {
        databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), null);
    }

    @Test
    public void writeEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writePreamble() throws Exception {
        database.setPreamble("Test preamble");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Preamble{Test preamble}" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writePreambleAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        database.setPreamble("Test preamble");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Preamble{Test preamble}" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEntry() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), new SavePreferences());

        assertEquals(Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}" + Globals.NEWLINE + Globals.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEncodingAndEntry() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        database.insertEntry(entry);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@Article{," + Globals.NEWLINE + "}"
                + Globals.NEWLINE + Globals.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}"
                + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEpilogue() throws Exception {
        database.setEpilog("Test epilog");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "Test epilog" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEpilogueAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        database.setEpilog("Test epilog");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "Test epilog" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeMetadata() throws Exception {
        DatabaseLabelPattern labelPattern = new DatabaseLabelPattern();
        labelPattern.setDefaultValue("test");
        metaData.setLabelPattern(labelPattern);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeMetadataAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        DatabaseLabelPattern labelPattern = new DatabaseLabelPattern();
        labelPattern.setDefaultValue("test");
        metaData.setLabelPattern(labelPattern);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE
                +
                "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeGroups() throws Exception {
        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup());
        groupRoot.addSubgroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING));
        metaData.setGroups(groupRoot);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        // @formatter:off
        assertEquals(Globals.NEWLINE
                + "@Comment{jabref-meta: groupstree:" + Globals.NEWLINE
                + "0 AllEntriesGroup:;" + Globals.NEWLINE
                + "1 ExplicitGroup:test\\;2\\;;" + Globals.NEWLINE
                + "}" + Globals.NEWLINE, session.getStringValue());
        // @formatter:on
    }

    @Test
    public void writeGroupsAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);

        GroupTreeNode groupRoot = GroupTreeNode.fromGroup(new AllEntriesGroup());
        groupRoot.addChild(GroupTreeNode.fromGroup(new ExplicitGroup("test", GroupHierarchyType.INCLUDING)));
        metaData.setGroups(groupRoot);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        // @formatter:off
        assertEquals(
                "% Encoding: US-ASCII" + Globals.NEWLINE +
                Globals.NEWLINE
                + "@Comment{jabref-meta: groupstree:" + Globals.NEWLINE
                + "0 AllEntriesGroup:;" + Globals.NEWLINE
                + "1 ExplicitGroup:test\\;2\\;;" + Globals.NEWLINE
                + "}" + Globals.NEWLINE, session.getStringValue());
        // @formatter:on
    }

    @Test
    public void writeString() throws Exception {
        database.addString(new BibtexString("id", "name", "content"));

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@String{name = {content}}" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeStringAndEncoding() throws Exception {
        SavePreferences preferences = new SavePreferences().withEncoding(Charsets.US_ASCII);
        database.addString(new BibtexString("id", "name", "content"));

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals("% Encoding: US-ASCII" + Globals.NEWLINE + Globals.NEWLINE +
                "@String{name = {content}}" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeEntryWithCustomizedTypeAlsoWritesTypeDeclaration() throws Exception {
        try {
            EntryTypes.addOrModifyCustomEntryType(new CustomEntryType("customizedType", "required", "optional"));
            BibEntry entry = new BibEntry();
            entry.setType("customizedType");
            database.insertEntry(entry);

            StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), new SavePreferences());

            assertEquals(
                    Globals.NEWLINE +
                            "@Customizedtype{," + Globals.NEWLINE + "}" + Globals.NEWLINE + Globals.NEWLINE
                            + "@Comment{jabref-meta: databaseType:bibtex;}"
                            + Globals.NEWLINE + Globals.NEWLINE
                            + "@Comment{jabref-entrytype: Customizedtype: req[required] opt[optional]}" + Globals.NEWLINE,
                    session.getStringValue());
        } finally {
            EntryTypes.removeAllCustomEntryTypes();
        }
    }

    @Test
    public void roundtrip() throws Exception {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormat.getReader(testBibtexFile, encoding));

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        StringSaveSession session = databaseWriter.savePartOfDatabase(context, result.getDatabase().getEntries(), preferences);
        try (Scanner scanner = new Scanner(testBibtexFile,encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), session.getStringValue());
        }
    }

    @Test
    public void roundtripWithUserComment() throws IOException {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormat.getReader(testBibtexFile, encoding));

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.writePartOfDatabase(stringWriter, context, result.getDatabase().getEntries(), preferences);
        try (Scanner scanner = new Scanner(testBibtexFile,encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    public void roundtripWithUserCommentAndEntryChange() throws IOException {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/bibWithUserComments.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormat.getReader(testBibtexFile, encoding));

        BibEntry entry = result.getDatabase().getEntryByKey("1137631").get();
        entry.setField("author", "Mr. Author");

        SavePreferences preferences = new SavePreferences().withEncoding(encoding).withSaveInOriginalOrder(true);
        BibDatabaseContext context = new BibDatabaseContext(result.getDatabase(), result.getMetaData(),
                new Defaults(BibDatabaseMode.BIBTEX));

        databaseWriter.writePartOfDatabase(stringWriter, context, result.getDatabase().getEntries(), preferences);

        try (Scanner scanner = new Scanner(Paths.get("src/test/resources/testbib/bibWithUserCommentAndEntryChange.bib"),encoding.name())) {
            assertEquals(scanner.useDelimiter("\\A").next(), stringWriter.toString());
        }
    }

    @Test
    public void roundtripWithUserCommentBeforeStringAndChange() throws IOException {
        Path testBibtexFile = Paths.get("src/test/resources/testbib/complex.bib");
        Charset encoding = StandardCharsets.UTF_8;
        ParserResult result = BibtexParser.parse(ImportFormat.getReader(testBibtexFile, encoding));

        BibtexString string = result.getDatabase().getStringValues().iterator().next();
        if(string.getContent().isEmpty()) {
            // do nothing
        } else {
            string.setContent("my first string");
        }

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

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.singletonList(entry), new SavePreferences());

        assertEquals("presaved serialization" + Globals.NEWLINE + "@Comment{jabref-meta: databaseType:bibtex;}"
                + Globals.NEWLINE, session.getStringValue());
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

        assertEquals(Globals.NEWLINE +
                        "@Article{," + Globals.NEWLINE + "  author = {Mr. author}," + Globals.NEWLINE + "}"
                        + Globals.NEWLINE + Globals.NEWLINE
                        + "@Comment{jabref-meta: databaseType:bibtex;}"
                        + Globals.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeSavedSerializationOfStringIfUnchanged() throws Exception {
        BibtexString string = new BibtexString("id", "name", "content");
        string.setParsedSerialization("serialization");
        database.addString(string);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals("serialization", session.getStringValue());
    }

    @Test
    public void reformatStringIfAskedToDoSo() throws Exception {
        BibtexString string = new BibtexString("id", "name", "content");
        string.setParsedSerialization("wrong serialization");
        database.addString(string);

        SavePreferences preferences = new SavePreferences().withReformatFile(true);
        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), preferences);

        assertEquals(Globals.NEWLINE + "@String{name = {content}}" + Globals.NEWLINE, session.getStringValue());

    }

    @Test
    public void writeSaveActions() throws Exception {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: saveActions:enabled;" + Globals.NEWLINE
                + "title[lower_case]" + Globals.NEWLINE + ";}" + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeSaveOrderConfig() throws Exception {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig(false, new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true),
                new SaveOrderConfig.SortCriterion("abstract", false));
        metaData.setSaveOrderConfig(saveOrderConfig);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE
                + "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"
                + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeCustomKeyPattern() throws Exception {
        AbstractLabelPattern pattern = new DatabaseLabelPattern();
        pattern.setDefaultValue("test");
        pattern.addLabelPattern("article", "articleTest");
        metaData.setLabelPattern(pattern);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: keypattern_article:articleTest;}" + Globals.NEWLINE
                        + Globals.NEWLINE + "@Comment{jabref-meta: keypatterndefault:test;}" + Globals.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeBiblatexMode() throws Exception {
        metaData.setMode(BibDatabaseMode.BIBLATEX);

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: databaseType:biblatex;}" + Globals.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeProtectedFlag() throws Exception {
        metaData.markAsProtected();

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: protectedFlag:true;}" + Globals.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeContentSelectors() throws Exception {
        metaData.setContentSelectors("title", Arrays.asList("testWord", "word2"));

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: selector_title:testWord;word2;}" + Globals.NEWLINE,
                session.getStringValue());
    }

    @Test
    public void writeFileDirectories() throws Exception {
        metaData.setDefaultFileDirectory("\\Literature\\");
        metaData.setUserFileDirectory("defaultOwner-user", "D:\\Documents");

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals(Globals.NEWLINE + "@Comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}" + Globals.NEWLINE +
                Globals.NEWLINE + "@Comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                + Globals.NEWLINE, session.getStringValue());
    }

    @Test
    public void writeNotEmptyContentSelectors() throws Exception {
        metaData.setContentSelectors("title", Collections.singletonList(""));

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals("", session.getStringValue());
    }

    @Test
    public void writeNotCompletelyEmptyContentSelectors() throws Exception {
        metaData.setContentSelectors("title", Collections.emptyList());

        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, Collections.emptyList(), new SavePreferences());

        assertEquals("", session.getStringValue());
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
                , session.getStringValue());
    }

    @Test
    public void writeEntriesInOriginalOrderWhenNoSaveOrderConfigIsSetInMetadata() throws Exception {
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
        StringSaveSession session = databaseWriter.savePartOfDatabase(bibtexContext, database.getEntries(), preferences);

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
                , session.getStringValue());
    }

}
