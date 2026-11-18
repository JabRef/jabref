package org.jabref.logic.cleanup;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
class BooktitleAbbreviationCleanupTest {
    private static final String FULL_NAME = "International Conference on Business Process Management";
    private final BibDatabase database = new BibDatabase();
    private final JournalAbbreviationRepository journals = new JournalAbbreviationRepository();
    private final ConferenceAbbreviationRepository conferences = new ConferenceAbbreviationRepository();

    // [utest->req~abbreviations.booktitle~1]
    @Test
    void abbreviatesAndExpandsIssueExample() {
        conferences.addCustomAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, FULL_NAME);
        AbbreviateJournalCleanup abbreviate = new AbbreviateJournalCleanup(database, journals, conferences, AbbreviationType.DEFAULT, true);
        UnabbreviateJournalCleanup expand = new UnabbreviateJournalCleanup(database, journals, conferences);

        assertEquals(List.of(new FieldChange(entry, StandardField.BOOKTITLE, FULL_NAME, "BPM")), abbreviate.cleanup(entry));
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, "BPM"), entry);
        assertEquals(List.of(), abbreviate.cleanup(entry));
        assertEquals(List.of(new FieldChange(entry, StandardField.BOOKTITLE, "BPM", FULL_NAME)), expand.cleanup(entry));
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, FULL_NAME), entry);
        assertEquals(List.of(), expand.cleanup(entry));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            DEFAULT,         Conf. Ex. Syst.
            DOTLESS,         Conf Ex Syst
            SHORTEST_UNIQUE,  CES
            """)
    void reusesJournalAbbreviationForms(AbbreviationType type, String expected) {
        conferences.addCustomAbbreviation(new Abbreviation("Conference on Example Systems", "Conf. Ex. Syst.", "CES"));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "Conference on Example Systems");

        new AbbreviateJournalCleanup(database, journals, conferences, type, false).cleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, expected), entry);
        new UnabbreviateJournalCleanup(database, journals, conferences).cleanup(entry);
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, "Conference on Example Systems"), entry);
    }

    @Test
    void usesSeparateRepositoriesAndKeepsFjournalSpecificToJournals() {
        journals.addCustomAbbreviation(new Abbreviation(FULL_NAME, "Journal"));
        conferences.addCustomAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, FULL_NAME)
                .withField(StandardField.JOURNALTITLE, FULL_NAME)
                .withField(StandardField.BOOKTITLE, FULL_NAME);

        new AbbreviateJournalCleanup(database, journals, conferences, AbbreviationType.DEFAULT, true).cleanup(entry);

        assertEquals(new BibEntry()
                .withField(StandardField.JOURNAL, "Journal")
                .withField(StandardField.JOURNALTITLE, "Journal")
                .withField(StandardField.BOOKTITLE, "BPM")
                .withField(AMSField.FJOURNAL, FULL_NAME), entry);
    }

    @Test
    void expansionDoesNotUseOrClearFjournalForBooktitle() {
        conferences.addCustomAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "BPM").withField(AMSField.FJOURNAL, "Journal full name");

        new UnabbreviateJournalCleanup(database, journals, conferences).cleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, FULL_NAME).withField(AMSField.FJOURNAL, "Journal full name"), entry);
    }

    @Test
    void journalRecordDoesNotAbbreviateBooktitle() {
        journals.addCustomAbbreviation(new Abbreviation(FULL_NAME, "Journal"));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, FULL_NAME);

        assertEquals(List.of(), new AbbreviateJournalCleanup(database, journals, conferences, AbbreviationType.DEFAULT, false).cleanup(entry));
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, FULL_NAME), entry);
    }

    @Test
    void unknownAbbreviationIsNotExpanded() {
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "Unknown");

        assertEquals(List.of(), new UnabbreviateJournalCleanup(database, journals, conferences).cleanup(entry));
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, "Unknown"), entry);
    }

    @Test
    void ltwaLeavesBooktitleUnchanged() {
        conferences.addCustomAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, FULL_NAME);

        assertEquals(List.of(), new AbbreviateJournalCleanup(database, journals, conferences, AbbreviationType.LTWA, false).cleanup(entry));
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, FULL_NAME), entry);
    }

    @Test
    void resolvesBibtexStringsBeforeAbbreviating() {
        conferences.addCustomAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        database.addString(new BibtexString("conference", FULL_NAME));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "#conference#");

        assertEquals(List.of(new FieldChange(entry, StandardField.BOOKTITLE, "#conference#", "BPM")),
                new AbbreviateJournalCleanup(database, journals, conferences, AbbreviationType.DEFAULT, false).cleanup(entry));
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, "BPM"), entry);
    }

    @Test
    void reusesFuzzyMatchingForConferenceTitles() {
        conferences.addCustomAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "International Conference on Business Process Managemen");

        new AbbreviateJournalCleanup(database, journals, conferences, AbbreviationType.DEFAULT, false).cleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, "BPM"), entry);
    }

    @Test
    void escapesAmpersandsWhenAbbreviatingAndExpanding() {
        conferences.addCustomAbbreviation(new Abbreviation("Conference on Research & Development", "R&D"));
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "Conference on Research & Development");

        new AbbreviateJournalCleanup(database, journals, conferences, AbbreviationType.DEFAULT, false).cleanup(entry);

        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, "R\\&D"), entry);
        new UnabbreviateJournalCleanup(database, journals, conferences).cleanup(entry);
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, "Conference on Research \\& Development"), entry);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            ABBREVIATE_DEFAULT,          International Conference on Business Process Management, BPM
            ABBREVIATE_DOTLESS,          International Conference on Business Process Management, BPM
            ABBREVIATE_SHORTEST_UNIQUE,  International Conference on Business Process Management, BPM
            UNABBREVIATE,               BPM, International Conference on Business Process Management
            """)
    void cleanupWorkerRoutesBooktitleToConferenceRepository(CleanupPreferences.CleanupStep step, String input, String expected) {
        conferences.addCustomAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        CleanupWorker worker = new CleanupWorker(new BibDatabaseContext(database), mock(FilePreferences.class), mock(TimestampPreferences.class), false, journals, conferences);
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, input);

        assertEquals(List.of(new FieldChange(entry, StandardField.BOOKTITLE, input, expected)), worker.cleanup(new CleanupPreferences(step), entry));
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, expected), entry);
    }
}
