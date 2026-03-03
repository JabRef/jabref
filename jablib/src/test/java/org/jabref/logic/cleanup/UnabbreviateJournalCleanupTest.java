package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class UnabbreviateJournalCleanupTest {
    private UnabbreviateJournalCleanup cleanup;
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
        Mockito.when(conferenceRepositoryMock.getFullName(anyString())).thenReturn(Optional.empty());

        cleanup = new UnabbreviateJournalCleanup(databaseMock, journalRepositoryMock, conferenceRepositoryMock);
    }

    @Test
    void noJournalFieldsMeansNoChange() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);
    }

    @Test
    void repositoryDoesNotRecognizeJournalNameNoChange() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Unknown Journal");
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Unknown Journal");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void journalAlreadyUnabbreviatedNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void unabbreviateJournalSuccessful() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void unabbreviateJournalTitleSuccessful() {
        Abbreviation abbreviation = new Abbreviation("Review Letters", "Rev. Lett.");
        Mockito.when(journalRepositoryMock.isKnownName("Rev. Lett.")).thenReturn(true);
        Mockito.when(journalRepositoryMock.isAbbreviatedName("Rev. Lett.")).thenReturn(true);
        Mockito.when(journalRepositoryMock.get("Rev. Lett.")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Rev. Lett.");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNALTITLE, "Rev. Lett.", "Review Letters")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void unabbreviateBookTitleSuccessful() {
        Mockito.when(conferenceRepositoryMock.getFullName("BPM"))
               .thenReturn(Optional.of("International Conference on Business Process Management"));

        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, "BPM");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.BOOKTITLE, "BPM", "International Conference on Business Process Management")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.BOOKTITLE, "International Conference on Business Process Management");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void unabbreviateBothJournalTitleAndJournalSuccessful() {
        Abbreviation abbreviationJournal = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviationJournal));

        Abbreviation abbreviationJournalTitle = new Abbreviation("Review Letters", "Rev. Lett.");
        Mockito.when(journalRepositoryMock.isKnownName("Rev. Lett.")).thenReturn(true);
        Mockito.when(journalRepositoryMock.isAbbreviatedName("Rev. Lett.")).thenReturn(true);
        Mockito.when(journalRepositoryMock.get("Rev. Lett.")).thenReturn(Optional.of(abbreviationJournalTitle));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo").withField(StandardField.JOURNALTITLE, "Rev. Lett.");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo"),
                new FieldChange(entry, StandardField.JOURNALTITLE, "Rev. Lett.", "Review Letters")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo")
                .withField(StandardField.JOURNALTITLE, "Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void restoreUnabbreviatedJournalTitleFromFJournal() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo")
                .withField(AMSField.FJOURNAL, "Journal of Foo");

        List<FieldChange> changes = cleanup.cleanup(entry);
        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, "Journal of Foo", null),
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void OnEmptyFJournalFieldShouldUseJournalAbbreviationRepository() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo")
                .withField(AMSField.FJOURNAL, "");

        Abbreviation abbreviationJournal = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviationJournal));

        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);

        verify(journalRepositoryMock, atLeastOnce()).get("J. Foo");
    }

    @Test
    void unabbreviateJournalTitleField() {
        Abbreviation abbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(journalRepositoryMock.isKnownName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(journalRepositoryMock.isAbbreviatedName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(journalRepositoryMock.get("Phys. Rev. Lett.")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Phys. Rev. Lett.");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNALTITLE, "Phys. Rev. Lett.", "Physical Review Letters")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "Physical Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void trailingWhitespaceIsTrimmedBeforeLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(journalRepositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(journalRepositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, " J. Foo ");
        Mockito.when(databaseMock.resolveForStrings(" J. Foo ")).thenReturn("J. Foo");

        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, " J. Foo ", "Journal of Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }
}
