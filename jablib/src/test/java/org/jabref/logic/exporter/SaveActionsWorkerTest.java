package org.jabref.logic.exporter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
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

public class SaveActionsWorkerTest {
    private BibDatabase database;
    private MetaData metaData;
    private BibDatabaseContext bibtexContext;
    private ImportFormatPreferences importFormatPreferences;
    private SelfContainedSaveConfiguration saveConfiguration;
    private FieldPreferences fieldPreferences;
    private FilePreferences filePreferences;
    private TimestampPreferences timestampPreferences;
    private CitationKeyPatternPreferences citationKeyPatternPreferences;
    private BibEntryTypesManager entryTypesManager;
    private SaveActionsWorker saveActionsWorker;
    private CliPreferences cliPreferences;
    private JournalAbbreviationRepository journalAbbreviationRepository;

    @BeforeEach
    void setUp() {
        fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = new BibEntryTypesManager();
        database = new BibDatabase();
        metaData = new MetaData();
        filePreferences = mock(FilePreferences.class);
        timestampPreferences = mock(TimestampPreferences.class);
        journalAbbreviationRepository = mock(JournalAbbreviationRepository.class);
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

        initializeSaveActionsWorker();
    }

    private void initializeSaveActionsWorker() {
        saveActionsWorker = new SaveActionsWorker(bibtexContext, filePreferences, timestampPreferences,
                fieldPreferences, false, journalAbbreviationRepository);
    }

    @Test
    void normalizeWhitespacesCleanupOnlyInTextFields() {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Firstname1 Lastname1   and   Firstname2 Lastname2")
                .withField(StandardField.FILE, "some  --  filename  -- spaces.pdf")
                .withChanged(true);

        saveActionsWorker.applySaveActions(firstEntry, metaData);

        assertEquals("""
                @Article{,
                  author = {Firstname1 Lastname1 and Firstname2 Lastname2},
                  file   = {some  --  filename  -- spaces.pdf},
                }
                """, firstEntry.getStringRepresentation(firstEntry, BibDatabaseMode.BIBLATEX, entryTypesManager, fieldPreferences));
    }

    @Test
    void applySaveActionsDoesNotTrimIfEntryNotChanged() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Firstname1 Lastname1   and   Firstname2 Lastname2")
                .withField(StandardField.FILE, "some  --  filename  -- spaces.pdf");

        saveActionsWorker.applySaveActions(entry, metaData);

        assertEquals("""
                @Article{,
                  author = {Firstname1 Lastname1   and   Firstname2 Lastname2},
                  file   = {some  --  filename  -- spaces.pdf},
                }
                """, entry.getStringRepresentation(entry, BibDatabaseMode.BIBLATEX, entryTypesManager, fieldPreferences));
    }

    @Test
    void trimFieldContents() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.NOTE, "        some note    \t")
                .withChanged(true);

        saveActionsWorker.applySaveActions(entry, metaData);

        assertEquals("""
                        @Article{,
                          note = {some note},
                        }
                        """,
                entry.getStringRepresentation(entry, BibDatabaseMode.BIBLATEX, entryTypesManager, fieldPreferences));
    }

    @Test
    void newlineAtEndOfAbstractFieldIsDeleted() {
        String text = "lorem ipsum lorem ipsum\nlorem ipsum lorem ipsum";

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.ABSTRACT, text + OS.NEWLINE);

        saveActionsWorker.applySaveActions(entry, metaData);

        assertEquals(String.format("""
                @Article{,
                  abstract = {%s},
                }
                """, text), entry.getStringRepresentation(entry, BibDatabaseMode.BIBLATEX, entryTypesManager, fieldPreferences));
    }

    @Test
    void appliesFieldFormatterCleanupsIfExistsInMetadata() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Firstname1 Lastname1 and Firstname2 Lastname2")
                .withField(StandardField.BOOKTITLE, "some title");

        FieldFormatterCleanupActions actions = new FieldFormatterCleanupActions(true, List.of(
                new FieldFormatterCleanup(StandardField.AUTHOR, new LowerCaseFormatter()),
                new FieldFormatterCleanup(StandardField.BOOKTITLE, new UpperCaseFormatter())));
        metaData.setFieldFormatterCleanupActions(actions);

        saveActionsWorker.applySaveActions(entry, metaData);

        assertEquals("""
                @Article{,
                  author    = {firstname1 lastname1 and firstname2 lastname2},
                  booktitle = {SOME TITLE},
                }
                """, entry.getStringRepresentation(entry, BibDatabaseMode.BIBLATEX, entryTypesManager, fieldPreferences));
    }

    @Test
    void appliesMultiFieldCleanupsIfExistsInMetadata() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.YEAR, "2026")
                .withField(StandardField.MONTH, "March")
                .withField(StandardField.DATE, "");

        metaData.setMultiFieldCleanups(Set.of(CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX));
        saveActionsWorker.applySaveActions(entry, metaData);

        assertEquals("""
                @Article{,
                  date = {2026-03},
                }
                """, entry.getStringRepresentation(entry, BibDatabaseMode.BIBLATEX, entryTypesManager, fieldPreferences));
    }

    @Test
    void appliesJournalAbbreviationCleanupsIfExistsInMetadata() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, "Journal of Something")
                .withField(StandardField.BOOKTITLE, "some title");

        metaData.setJournalAbbreviationCleanup(CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT);

        when(journalAbbreviationRepository.get("Journal of Something")).thenReturn(Optional.of(new Abbreviation("name", "abbreviated")));
        saveActionsWorker.applySaveActions(entry, metaData);

        assertEquals("""
                @Article{,
                  booktitle = {some title},
                  journal   = {abbreviated},
                }
                """, entry.getStringRepresentation(entry, BibDatabaseMode.BIBLATEX, entryTypesManager, fieldPreferences));
    }
}
