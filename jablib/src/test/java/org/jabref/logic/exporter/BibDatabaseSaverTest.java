package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BibDatabaseSaverTest {
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
    private BibDatabaseSaver bibDatabaseSaver;
    private CliPreferences cliPreferences;

    @BeforeEach
    void setUp() {
        fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = new BibEntryTypesManager();
        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        database = new BibDatabase();
        metaData = new MetaData();
        bibtexContext = new BibDatabaseContext(database, metaData);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences()).thenReturn(fieldPreferences);

        cliPreferences = mock(CliPreferences.class);
        JournalAbbreviationPreferences journalAbbreviationPreferences = mock(JournalAbbreviationPreferences.class);
        when(journalAbbreviationPreferences.shouldUseFJournalField()).thenReturn(false);
        when(cliPreferences.getJournalAbbreviationPreferences()).thenReturn(journalAbbreviationPreferences);
        when(cliPreferences.getFieldPreferences()).thenReturn(fieldPreferences);
        when(cliPreferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);
        when(cliPreferences.getCustomEntryTypesRepository()).thenReturn(entryTypesManager);

        initializeBibDatabaseSaver();
    }

    private void initializeBibDatabaseSaver() {
        bibDatabaseSaver = new BibDatabaseSaver(bibWriter, saveConfiguration, cliPreferences, entryTypesManager);
    }

    @Test
    void normalizeWhitespacesCleanupOnlyInTextFields() throws IOException {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Firstname1 Lastname1   and   Firstname2 Lastname2")
                .withField(StandardField.FILE, "some  --  filename  -- spaces.pdf")
                .withChanged(true);

        database.insertEntry(firstEntry);

        bibDatabaseSaver.saveDatabase(bibtexContext);

        assertEquals("""
                @Article{,
                  author = {Firstname1 Lastname1 and Firstname2 Lastname2},
                  file   = {some  --  filename  -- spaces.pdf},
                }
                """.replace("\n", OS.NEWLINE), stringWriter.toString());
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
        initializeBibDatabaseSaver();
        bibDatabaseSaver.saveDatabase(context);
        assertEquals(Files.readString(Path.of("src/test/resources/testbib/bibWithUserCommentAndEntryChange.bib"), encoding), stringWriter.toString());
    }

    @Test
    void writeFieldFormatterCleanupActions() throws IOException {
        FieldFormatterCleanupActions saveActions = new FieldFormatterCleanupActions(true,
                Arrays.asList(
                        new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter()),
                        new FieldFormatterCleanup(StandardField.JOURNAL, new TitleCaseFormatter()),
                        new FieldFormatterCleanup(StandardField.DAY, new UpperCaseFormatter())));
        metaData.setFieldFormatterCleanupActions(saveActions);

        bibDatabaseSaver.saveDatabase(bibtexContext);

        // The order should be kept (the cleanups are a list, not a set)
        assertEquals("@Comment{jabref-meta: fieldFormatterCleanupActions:enabled;"
                + OS.NEWLINE
                + "title[lower_case]" + OS.NEWLINE
                + "journal[title_case]" + OS.NEWLINE
                + "day[upper_case]" + OS.NEWLINE
                + ";}"
                + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void trimFieldContents() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.NOTE, "        some note    \t")
                .withChanged(true);
        database.insertEntry(entry);

        bibDatabaseSaver.saveDatabase(bibtexContext);

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

        bibDatabaseSaver.saveDatabase(bibtexContext);

        assertEquals("@Article{," + OS.NEWLINE +
                        "  abstract = {" + text + "}," + OS.NEWLINE +
                        "}" + OS.NEWLINE,
                stringWriter.toString());
    }
}
