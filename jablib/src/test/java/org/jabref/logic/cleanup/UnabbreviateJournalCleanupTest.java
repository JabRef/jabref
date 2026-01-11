package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

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
        Mockito.when(repositoryMock.get(anyString())).thenReturn(Optional.empty());

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
        assertEquals(Optional.of("Unknown Journal"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void journalAlreadyUnabbreviatedNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);
        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void unabbreviateJournalSuccessful() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void unabbreviateJournalTitleSuccessful() {
        Abbreviation abbreviation = new Abbreviation("Review Letters", "Rev. Lett.");
        Mockito.when(repositoryMock.isKnownName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.get("Rev. Lett.")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Rev. Lett.");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNALTITLE, "Rev. Lett.", "Review Letters")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Review Letters"), entry.getField(StandardField.JOURNALTITLE));
    }

    @Test
    void unabbreviateBothJournalTitleAndJournalSuccessful() {
        Abbreviation abbreviationJournal = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviationJournal));

        Abbreviation abbreviationJournalTitle = new Abbreviation("Review Letters", "Rev. Lett.");
        Mockito.when(repositoryMock.isKnownName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.get("Rev. Lett.")).thenReturn(Optional.of(abbreviationJournalTitle));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo").withField(StandardField.JOURNALTITLE, "Rev. Lett.");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo"),
                new FieldChange(entry, StandardField.JOURNALTITLE, "Rev. Lett.", "Review Letters")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Review Letters"), entry.getField(StandardField.JOURNALTITLE));
    }

    @Test
    void restoreFromFJournal() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo")
                .withField(AMSField.FJOURNAL, "Journal of Foo");

        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, "Journal of Foo", ""),
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.empty(), entry.getField(AMSField.FJOURNAL));
    }

    @Test
    void unabbreviateJournalTitleField() {
        Abbreviation abbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(repositoryMock.isKnownName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.get("Phys. Rev. Lett.")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Phys. Rev. Lett.");
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNALTITLE, "Phys. Rev. Lett.", "Physical Review Letters")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Physical Review Letters"), entry.getField(StandardField.JOURNALTITLE));
    }

    @Test
    void trailingWhitespaceIsTrimmedBeforeLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, " J. Foo ");
        Mockito.when(databaseMock.resolveForStrings(" J. Foo ")).thenReturn("J. Foo");

        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, " J. Foo ", "Journal of Foo")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
    }
}
