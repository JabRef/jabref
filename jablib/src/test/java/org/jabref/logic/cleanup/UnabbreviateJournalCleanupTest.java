package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

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
    private JournalAbbreviationRepository repositoryMock;
    private BibDatabase databaseMock;

    @BeforeEach
    void setUp() {
        databaseMock = Mockito.mock(BibDatabase.class);
        Mockito.when(databaseMock.resolveForStrings(anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        repositoryMock = Mockito.mock(JournalAbbreviationRepository.class);
        Mockito.when(repositoryMock.isKnownName(anyString())).thenReturn(false);
        Mockito.when(repositoryMock.isAbbreviatedName(anyString())).thenReturn(false);
        Mockito.when(repositoryMock.getFullName(anyString())).thenReturn(Optional.empty());

        cleanup = new UnabbreviateJournalCleanup(databaseMock, repositoryMock);
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
        Mockito.when(repositoryMock.isKnownName("Journal of Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Journal of Foo")).thenReturn(false);

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void unabbreviateJournalSuccessful() {
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("J. Foo")).thenReturn(Optional.of("Journal of Foo"));

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
        Mockito.when(repositoryMock.isKnownName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("Rev. Lett.")).thenReturn(Optional.of("Review Letters"));

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
    void unabbreviateBothJournalTitleAndJournalSuccessful() {
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("J. Foo")).thenReturn(Optional.of("Journal of Foo"));

        Mockito.when(repositoryMock.isKnownName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("Rev. Lett.")).thenReturn(Optional.of("Review Letters"));

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

        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("J. Foo")).thenReturn(Optional.of("Journal of Foo"));

        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);

        verify(repositoryMock, atLeastOnce()).getFullName("J. Foo");
    }

    @Test
    void unabbreviateJournalTitleField() {
        Mockito.when(repositoryMock.isKnownName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("Phys. Rev. Lett.")).thenReturn(Optional.of("Physical Review Letters"));

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
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("J. Foo")).thenReturn(Optional.of("Journal of Foo"));
        Mockito.when(databaseMock.resolveForStrings(" J. Foo ")).thenReturn("J. Foo");

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, " J. Foo ");

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
