/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.integrity;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGeneratorTestUtils;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupWorker;
import org.jabref.logic.conferences.ConferenceAbbreviationLoader;
import org.jabref.logic.conferences.ConferenceAbbreviationPreferences;
import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
@ResourceLock("Localization.lang")
class ConferenceAbbreviationIntegrationTest {
    private static final String FULL_NAME = "International Conference on Business Process Management";

    private final BibDatabase database = new BibDatabase();
    private final BibDatabaseContext context = new BibDatabaseContext(database);
    private final JournalAbbreviationRepository journals = new JournalAbbreviationRepository();
    private ConferenceAbbreviationRepository conferences;

    @BeforeEach
    void loadExampleCsv() throws URISyntaxException {
        Path csv = Path.of(ConferenceAbbreviationIntegrationTest.class.getResource(
                "/org/jabref/logic/conferences/conference_abbreviations_example.csv").toURI());
        conferences = ConferenceAbbreviationLoader.loadRepository(new ConferenceAbbreviationPreferences(List.of(csv.toString())));
    }

    @Test
    void loadsIssueExampleUsingJournalCsvFormat() {
        assertEquals(List.of(new Abbreviation(FULL_NAME, "BPM")), List.copyOf(conferences.getCustomAbbreviations()));
        assertEquals(List.of(), List.copyOf(journals.getCustomAbbreviations()));
    }

    // [utest->req~abbreviations.conference-integrity~1]
    @Test
    void csvSupportsAbbreviationIntegrityCheckAndExpansion() {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.BOOKTITLE, FULL_NAME);
        database.insertEntry(entry);
        CleanupWorker worker = new CleanupWorker(context, mock(FilePreferences.class), mock(TimestampPreferences.class), false, journals, conferences);
        IntegrityCheck check = createIntegrityCheck();

        assertEquals(List.of(), booktitleMessages(check));
        assertEquals(List.of(new FieldChange(entry, StandardField.BOOKTITLE, FULL_NAME, "BPM")),
                worker.cleanup(new CleanupPreferences(CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT), entry));
        BibEntry abbreviatedEntry = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.BOOKTITLE, "BPM");
        assertEquals(abbreviatedEntry, entry);

        assertEquals(List.of(new IntegrityMessage(Localization.lang("abbreviation detected"), entry, StandardField.BOOKTITLE)), booktitleMessages(check));
        assertEquals(abbreviatedEntry, entry);

        assertEquals(List.of(new FieldChange(entry, StandardField.BOOKTITLE, "BPM", FULL_NAME)),
                worker.cleanup(new CleanupPreferences(CleanupPreferences.CleanupStep.UNABBREVIATE), entry));
        assertEquals(new BibEntry(StandardEntryType.InProceedings).withField(StandardField.BOOKTITLE, FULL_NAME), entry);
        assertEquals(List.of(), booktitleMessages(check));
    }

    @Test
    void journalOnlyAbbreviationDoesNotTriggerBooktitleWarning() {
        journals.addCustomAbbreviation(new Abbreviation("Journal of Example Research", "J. Ex. Res."));
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.BOOKTITLE, "J. Ex. Res.");
        database.insertEntry(entry);

        assertEquals(List.of(), booktitleMessages(createIntegrityCheck()));
    }

    @Test
    void conferenceAbbreviationInJournalFieldDoesNotTriggerBooktitleWarning() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "BPM");
        database.insertEntry(entry);

        assertEquals(List.of(), new AbbreviationChecker(conferences).check(entry));
    }

    private IntegrityCheck createIntegrityCheck() {
        return new IntegrityCheck(context, mock(FilePreferences.class), CitationKeyGeneratorTestUtils.getInstanceForTesting(), journals, conferences, false);
    }

    private List<IntegrityMessage> booktitleMessages(IntegrityCheck check) {
        return check.check().stream()
                    .filter(message -> message.field() == StandardField.BOOKTITLE)
                    .toList();
    }
}
