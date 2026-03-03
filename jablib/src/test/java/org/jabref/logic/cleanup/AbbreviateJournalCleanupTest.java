package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

public class AbbreviateJournalCleanupTest {
    private AbbreviateJournalCleanup cleanupWithoutFJournal;
    private AbbreviateJournalCleanup cleanupWithFJournal;

    private JournalAbbreviationRepository journalRepositoryMock;
    private ConferenceAbbreviationRepository conferenceRepositoryMock;
    private BibDatabase databaseMock;

    @BeforeEach
    void setUp() {
        databaseMock = Mockito.mock(BibDatabase.class);
        Mockito.when(databaseMock.resolveForStrings(anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        journalRepositoryMock = Mockito.mock(JournalAbbreviationRepository.class);
        conferenceRepositoryMock = Mockito.mock(ConferenceAbbreviationRepository.class);

        Mockito.when(journalRepositoryMock.get(anyString())).thenReturn(Optional.empty());
        Mockito.when(conferenceRepositoryMock.getAbbreviation(anyString())).thenReturn(Optional.empty());

        cleanupWithoutFJournal = new AbbreviateJournalCleanup(
                databaseMock,
                journalRepositoryMock,
                conferenceRepositoryMock,
                AbbreviationType.DEFAULT,
                false
        );
        cleanupWithFJournal = new AbbreviateJournalCleanup(
                databaseMock,
                journalRepositoryMock,
                conferenceRepositoryMock,
                AbbreviationType.DEFAULT,
                true
        );
    }

    @Test
    void repositoryDoesNotRecognizeJournalNameNoChange() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Some Unknown Journal");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Some Unknown Journal");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalSuccessfulWithFJournalFalse() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Foo", "J. Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalAlreadyAbbreviatedNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalAndSetFJournal() {
        Abbreviation abbreviation = new Abbreviation("Canadian Journal of Math", "Can. J. Math.");
        Mockito.when(journalRepositoryMock.get("Canadian Journal of Math")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Canadian Journal of Math");
        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, null, "Canadian Journal of Math"),
                new FieldChange(entry, StandardField.JOURNAL, "Canadian Journal of Math", "Can. J. Math.")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Can. J. Math.")
                .withField(AMSField.FJOURNAL, "Canadian Journal of Math");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalTitleField() {
        Abbreviation abbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(journalRepositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Physical Review Letters");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNALTITLE, "Physical Review Letters", "Phys. Rev. Lett.")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "Phys. Rev. Lett.");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateBookTitleField() {
        Mockito.when(conferenceRepositoryMock.getAbbreviation("International Conference on Business Process Management"))
               .thenReturn(Optional.of("BPM"));

        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "International Conference on Business Process Management");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);
        
        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.BOOKTITLE, "International Conference on Business Process Management", "BPM")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, "BPM");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalTitleAndBookTitleInOneRun() {
        Abbreviation journalAbbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(journalRepositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(journalAbbreviation));
        Mockito.when(conferenceRepositoryMock.getAbbreviation("International Conference on Business Process Management"))
               .thenReturn(Optional.of("BPM"));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "Physical Review Letters")
                .withField(StandardField.BOOKTITLE, "International Conference on Business Process Management");
        
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);
        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNALTITLE, "Physical Review Letters", "Phys. Rev. Lett."),
                new FieldChange(entry, StandardField.BOOKTITLE, "International Conference on Business Process Management", "BPM")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "Phys. Rev. Lett.")
                .withField(StandardField.BOOKTITLE, "BPM");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void resolveForStringsIsCalled() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        cleanupWithoutFJournal.cleanup(entry);

        Mockito.verify(databaseMock).resolveForStrings("Journal of Foo");
    }

    @Test
    void oldFJournalIsOverwrittenWhenUseFJournalIsTrue() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo")
                .withField(AMSField.FJOURNAL, "Some old text");

        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, "Some old text", "Journal of Foo"),
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Foo", "J. Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo")
                .withField(AMSField.FJOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void braceAroundJournalNameIsIgnoredIfWanted() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "{Journal of Foo}");
        Mockito.when(databaseMock.resolveForStrings("{Journal of Foo}")).thenReturn("Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "{Journal of Foo}", "J. Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void trailingWhitespaceIsTrimmedBeforeLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo   ");
        Mockito.when(databaseMock.resolveForStrings("Journal of Foo   ")).thenReturn("Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Foo   ", "J. Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void databaseResolvesStringsDifferentlyAffectsLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));
        Mockito.when(databaseMock.resolveForStrings("MACRO_JOURNAL")).thenReturn("Journal of Foo");

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "MACRO_JOURNAL");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "MACRO_JOURNAL", "J. Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void repositoryReturnsSameStringNoChanges() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "Journal of Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        assertEquals(List.of(), changes);
    }

    @Test
    void journalTitleAndJournalBothRecognized() {
        Abbreviation abbreviationJournal = new Abbreviation("Journal of Bar", "J. Bar");
        Abbreviation abbreviationTitle = new Abbreviation("Review Letters", "Rev. Lett.");

        Mockito.when(journalRepositoryMock.get("Journal of Bar")).thenReturn(Optional.of(abbreviationJournal));
        Mockito.when(journalRepositoryMock.get("Review Letters")).thenReturn(Optional.of(abbreviationTitle));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Bar")
                .withField(StandardField.JOURNALTITLE, "Review Letters");

        AbbreviateJournalCleanup testCleanup = new AbbreviateJournalCleanup(
                databaseMock,
                journalRepositoryMock,
                conferenceRepositoryMock,
                AbbreviationType.DEFAULT,
                true
        );
        List<FieldChange> changes = testCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, null, "Journal of Bar"),
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Bar", "J. Bar"),
                // There is only one FJOURNAL field for an entry, which is why the test asserts this.
                // JOURNAL and JOURNALTITLE aren't meant to be this different for the same entry.
                new FieldChange(entry, AMSField.FJOURNAL, "Journal of Bar", "Review Letters"),
                new FieldChange(entry, StandardField.JOURNALTITLE, "Review Letters", "Rev. Lett.")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Bar")
                .withField(StandardField.JOURNALTITLE, "Rev. Lett.")
                .withField(AMSField.FJOURNAL, "Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalDotlessNoFjournal() {
        Abbreviation abbr = new Abbreviation("Long Name", "L. N.", "LN");
        Mockito.when(journalRepositoryMock.get("Long Name")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = new AbbreviateJournalCleanup(
                databaseMock,
                journalRepositoryMock,
                conferenceRepositoryMock,
                AbbreviationType.DOTLESS,
                false
        );

        List<FieldChange> changes = dotlessCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Long Name", "L N")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "L N");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalDotlessWithFjournal() {
        Abbreviation abbr = new Abbreviation("Long Name", "L. N.", "LN");
        Mockito.when(journalRepositoryMock.get("Long Name")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = new AbbreviateJournalCleanup(
                databaseMock,
                journalRepositoryMock,
                conferenceRepositoryMock,
                AbbreviationType.DOTLESS,
                true
        );

        List<FieldChange> changes = dotlessCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, null, "Long Name"),
                new FieldChange(entry, StandardField.JOURNAL, "Long Name", "L N")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "L N")
                .withField(AMSField.FJOURNAL, "Long Name");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalShortestUniqueNoFjournal() {
        Abbreviation abbr = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.", "PRL");
        Mockito.when(journalRepositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = new AbbreviateJournalCleanup(
                databaseMock,
                journalRepositoryMock,
                conferenceRepositoryMock,
                AbbreviationType.SHORTEST_UNIQUE,
                false
        );

        List<FieldChange> changes = shortestCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Physical Review Letters", "PRL")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "PRL");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalShortestUniqueWithFjournal() {
        Abbreviation abbr = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.", "PRL");
        Mockito.when(journalRepositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = new AbbreviateJournalCleanup(
                databaseMock,
                journalRepositoryMock,
                conferenceRepositoryMock,
                AbbreviationType.SHORTEST_UNIQUE,
                true
        );

        List<FieldChange> changes = shortestCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, null, "Physical Review Letters"),
                new FieldChange(entry, StandardField.JOURNAL, "Physical Review Letters", "PRL")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "PRL")
                .withField(AMSField.FJOURNAL, "Physical Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void ampersandStaysEscaped() {
        Abbreviation abbr = new Abbreviation("Aachen & Berlin", "A & B", "AB");
        Mockito.when(journalRepositoryMock.get("A & B")).thenReturn(Optional.of(abbr));
        Mockito.when(journalRepositoryMock.get("A \\& B")).thenReturn(Optional.of(abbr));
        Mockito.when(journalRepositoryMock.get("Aachen & Berlin")).thenReturn(Optional.of(abbr));
        Mockito.when(journalRepositoryMock.get("Aachen \\& Berlin")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "A \\& B");
        BibEntry expected = new BibEntry(entry).withField(AMSField.FJOURNAL, "Aachen \\& Berlin");
        new UnabbreviateJournalCleanup(databaseMock, journalRepositoryMock, conferenceRepositoryMock).cleanup(entry);
        new AbbreviateJournalCleanup(databaseMock, journalRepositoryMock, conferenceRepositoryMock, AbbreviationType.DEFAULT, true).cleanup(entry);
        assertEquals(expected, entry);
    }
}
