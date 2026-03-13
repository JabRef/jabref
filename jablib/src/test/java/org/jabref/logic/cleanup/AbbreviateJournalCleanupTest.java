package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

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

    private JournalAbbreviationRepository repositoryMock;
    private BibDatabase databaseMock;

    @BeforeEach
    void setUp() {
        databaseMock = Mockito.mock(BibDatabase.class);
        Mockito.when(databaseMock.resolveForStrings(anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        repositoryMock = Mockito.mock(JournalAbbreviationRepository.class);
        Mockito.when(repositoryMock.getDefaultAbbreviation(anyString())).thenReturn(Optional.empty());
        Mockito.when(repositoryMock.getDotless(anyString())).thenReturn(Optional.empty());
        Mockito.when(repositoryMock.getShortestUniqueAbbreviation(anyString())).thenReturn(Optional.empty());
        Mockito.when(repositoryMock.getFullName(anyString())).thenReturn(Optional.empty());

        cleanupWithoutFJournal = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DEFAULT, false);
        cleanupWithFJournal = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DEFAULT, true);
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
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Foo")).thenReturn(Optional.of("J. Foo"));

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
        Mockito.when(repositoryMock.getDefaultAbbreviation("J. Foo")).thenReturn(Optional.of("J. Foo"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void abbreviateJournalAndSetFJournal() {
        Mockito.when(repositoryMock.getDefaultAbbreviation("Canadian Journal of Math")).thenReturn(Optional.of("Can. J. Math."));
        Mockito.when(repositoryMock.getFullName("Canadian Journal of Math")).thenReturn(Optional.of("Canadian Journal of Math"));

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
        Mockito.when(repositoryMock.getDefaultAbbreviation("Physical Review Letters")).thenReturn(Optional.of("Phys. Rev. Lett."));

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
    void resolveForStringsIsCalled() {
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Foo")).thenReturn(Optional.of("J. Foo"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        cleanupWithoutFJournal.cleanup(entry);

        Mockito.verify(databaseMock).resolveForStrings("Journal of Foo");
    }

    @Test
    void oldFJournalIsOverwrittenWhenUseFJournalIsTrue() {
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Foo")).thenReturn(Optional.of("J. Foo"));
        Mockito.when(repositoryMock.getFullName("Journal of Foo")).thenReturn(Optional.of("Journal of Foo"));

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
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Foo")).thenReturn(Optional.of("J. Foo"));
        Mockito.when(databaseMock.resolveForStrings("{Journal of Foo}")).thenReturn("Journal of Foo");

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "{Journal of Foo}");

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
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Foo")).thenReturn(Optional.of("J. Foo"));
        Mockito.when(databaseMock.resolveForStrings("Journal of Foo   ")).thenReturn("Journal of Foo");

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo   ");

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
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Foo")).thenReturn(Optional.of("J. Foo"));
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
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Foo")).thenReturn(Optional.of("Journal of Foo"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        assertEquals(List.of(), changes);
    }

    @Test
    void journalTitleAndJournalBothRecognized() {
        Mockito.when(repositoryMock.getDefaultAbbreviation("Journal of Bar")).thenReturn(Optional.of("J. Bar"));
        Mockito.when(repositoryMock.getFullName("Journal of Bar")).thenReturn(Optional.of("Journal of Bar"));
        Mockito.when(repositoryMock.getDefaultAbbreviation("Review Letters")).thenReturn(Optional.of("Rev. Lett."));
        Mockito.when(repositoryMock.getFullName("Review Letters")).thenReturn(Optional.of("Review Letters"));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Bar")
                .withField(StandardField.JOURNALTITLE, "Review Letters");

        AbbreviateJournalCleanup testCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DEFAULT, true);
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
        Mockito.when(repositoryMock.getDotless("Long Name")).thenReturn(Optional.of("L N"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DOTLESS, false);

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
        Mockito.when(repositoryMock.getDotless("Long Name")).thenReturn(Optional.of("L N"));
        Mockito.when(repositoryMock.getFullName("Long Name")).thenReturn(Optional.of("Long Name"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DOTLESS, true);

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
        Mockito.when(repositoryMock.getShortestUniqueAbbreviation("Physical Review Letters")).thenReturn(Optional.of("PRL"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.SHORTEST_UNIQUE, false);

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
        Mockito.when(repositoryMock.getShortestUniqueAbbreviation("Physical Review Letters")).thenReturn(Optional.of("PRL"));
        Mockito.when(repositoryMock.getFullName("Physical Review Letters")).thenReturn(Optional.of("Physical Review Letters"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.SHORTEST_UNIQUE, true);

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
        Mockito.when(repositoryMock.isKnownName("A \\& B")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("A \\& B")).thenReturn(true);
        Mockito.when(repositoryMock.getFullName("A \\& B")).thenReturn(Optional.of("Aachen & Berlin"));
        Mockito.when(repositoryMock.getDefaultAbbreviation("Aachen \\& Berlin")).thenReturn(Optional.of("A & B"));
        Mockito.when(repositoryMock.getFullName("Aachen \\& Berlin")).thenReturn(Optional.of("Aachen & Berlin"));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "A \\& B");
        BibEntry expected = new BibEntry(entry).withField(AMSField.FJOURNAL, "Aachen \\& Berlin");
        new UnabbreviateJournalCleanup(databaseMock, repositoryMock).cleanup(entry);
        new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DEFAULT, true).cleanup(entry);
        assertEquals(expected, entry);
    }
}
