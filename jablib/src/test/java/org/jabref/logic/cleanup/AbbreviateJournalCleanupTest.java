package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

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

    private JournalAbbreviationRepository repositoryMock;
    private BibDatabase databaseMock;

    @BeforeEach
    void setUp() {
        databaseMock = Mockito.mock(BibDatabase.class);
        Mockito.when(databaseMock.resolveForStrings(anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        repositoryMock = Mockito.mock(JournalAbbreviationRepository.class);
        Mockito.when(repositoryMock.get(anyString())).thenReturn(Optional.empty());

        cleanupWithoutFJournal = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DEFAULT, false);
        cleanupWithFJournal = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DEFAULT, true);
    }

    @Test
    void repositoryDoesNotRecognizeJournalNameNoChange() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Some Unknown Journal");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertEquals(List.of(), changes);
        assertEquals(Optional.of("Some Unknown Journal"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalSuccessfulWithFJournalFalse() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Foo", "J. Foo")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("J. Foo"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.empty(), entry.getField(AMSField.FJOURNAL));
    }

    @Test
    void abbreviateJournalAlreadyAbbreviatedNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertEquals(List.of(), changes);
        assertEquals(Optional.of("J. Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalAndSetFJournal() {
        Abbreviation abbreviation = new Abbreviation("Canadian Journal of Math", "Can. J. Math.");
        Mockito.when(repositoryMock.get("Canadian Journal of Math")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Canadian Journal of Math");
        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, null, "Canadian Journal of Math"),
                new FieldChange(entry, StandardField.JOURNAL, "Canadian Journal of Math", "Can. J. Math.")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Can. J. Math."), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Canadian Journal of Math"), entry.getField(AMSField.FJOURNAL));
    }

    @Test
    void abbreviateJournalTitleField() {
        Abbreviation abbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(repositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Physical Review Letters");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNALTITLE, "Physical Review Letters", "Phys. Rev. Lett.")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("Phys. Rev. Lett."), entry.getField(StandardField.JOURNALTITLE));
    }

    @Test
    void resolveForStringsIsCalled() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        cleanupWithoutFJournal.cleanup(entry);

        Mockito.verify(databaseMock).resolveForStrings("Journal of Foo");
    }

    @Test
    void oldFJournalIsOverwrittenWhenUseFJournalIsTrue() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo")
                .withField(AMSField.FJOURNAL, "Some old text");

        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, "Some old text", "Journal of Foo"),
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Foo", "J. Foo")
        );
        assertEquals(expected, changes);
    }

    @Test
    void braceAroundJournalNameIsIgnoredIfWanted() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "{Journal of Foo}");
        Mockito.when(databaseMock.resolveForStrings("{Journal of Foo}")).thenReturn("Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "{Journal of Foo}", "J. Foo")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("J. Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void trailingWhitespaceIsTrimmedBeforeLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo   ");
        Mockito.when(databaseMock.resolveForStrings("Journal of Foo   ")).thenReturn("Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Foo   ", "J. Foo")
        );
        assertEquals(expected, changes);
    }

    @Test
    void databaseResolvesStringsDifferentlyAffectsLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));
        Mockito.when(databaseMock.resolveForStrings("MACRO_JOURNAL")).thenReturn("Journal of Foo");

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "MACRO_JOURNAL");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "MACRO_JOURNAL", "J. Foo")
        );
        assertEquals(expected, changes);
    }

    @Test
    void repositoryReturnsSameStringNoChanges() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "Journal of Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        assertEquals(List.of(), changes);
    }

    @Test
    void journalTitleAndJournalBothRecognized() {
        Abbreviation abbreviationJournal = new Abbreviation("Journal of Bar", "J. Bar");
        Abbreviation abbreviationTitle = new Abbreviation("Review Letters", "Rev. Lett.");

        Mockito.when(repositoryMock.get("Journal of Bar")).thenReturn(Optional.of(abbreviationJournal));
        Mockito.when(repositoryMock.get("Review Letters")).thenReturn(Optional.of(abbreviationTitle));

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
    }

    @Test
    void abbreviateJournalDotlessNoFjournal() {
        Abbreviation abbr = new Abbreviation("Long Name", "L. N.", "LN");
        Mockito.when(repositoryMock.get("Long Name")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DOTLESS, false);

        List<FieldChange> changes = dotlessCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Long Name", "L N")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("L N"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.empty(), entry.getField(AMSField.FJOURNAL));
    }

    @Test
    void abbreviateJournalDotlessWithFjournal() {
        Abbreviation abbr = new Abbreviation("Long Name", "L. N.", "LN");
        Mockito.when(repositoryMock.get("Long Name")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DOTLESS, true);

        List<FieldChange> changes = dotlessCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, null, "Long Name"),
                new FieldChange(entry, StandardField.JOURNAL, "Long Name", "L N")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("L N"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Long Name"), entry.getField(AMSField.FJOURNAL));
    }

    @Test
    void abbreviateJournalShortestUniqueNoFjournal() {
        Abbreviation abbr = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.", "PRL");
        Mockito.when(repositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.SHORTEST_UNIQUE, false);

        List<FieldChange> changes = shortestCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Physical Review Letters", "PRL")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("PRL"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.empty(), entry.getField(AMSField.FJOURNAL));
    }

    @Test
    void abbreviateJournalShortestUniqueWithFjournal() {
        Abbreviation abbr = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.", "PRL");
        Mockito.when(repositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.SHORTEST_UNIQUE, true);

        List<FieldChange> changes = shortestCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, AMSField.FJOURNAL, null, "Physical Review Letters"),
                new FieldChange(entry, StandardField.JOURNAL, "Physical Review Letters", "PRL")
        );
        assertEquals(expected, changes);

        assertEquals(Optional.of("PRL"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Physical Review Letters"), entry.getField(AMSField.FJOURNAL));
    }
}
