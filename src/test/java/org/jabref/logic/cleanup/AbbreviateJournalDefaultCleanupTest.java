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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

class AbbreviateJournalDefaultCleanupTest {

    private AbbreviateJournalDefaultCleanup cleanupWithoutFJournal;
    private AbbreviateJournalDefaultCleanup cleanupWithFJournal;
    private JournalAbbreviationRepository repositoryMock;
    private BibDatabase databaseMock;

    @BeforeEach
    void setUp() {
        databaseMock = Mockito.mock(BibDatabase.class);

        Mockito.when(databaseMock.resolveForStrings(anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        repositoryMock = Mockito.mock(JournalAbbreviationRepository.class);
        Mockito.when(repositoryMock.get(Mockito.anyString())).thenReturn(Optional.empty());

        cleanupWithoutFJournal = new AbbreviateJournalDefaultCleanup(databaseMock, repositoryMock, false);
        cleanupWithFJournal = new AbbreviateJournalDefaultCleanup(databaseMock, repositoryMock, true);
    }

    @Test
    void noJournalFieldsMeansNoChange() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);
        assertTrue(changes.isEmpty(), "Expected no changes when neither field is present");
    }

    @Test
    void repositoryDoesNotRecognizeJournalNameNoChange() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Some Unknown Journal");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertTrue(changes.isEmpty(), "No changes expected if the repo can't find an abbreviation");
        assertEquals(Optional.of("Some Unknown Journal"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalSuccessfulWithFJournalFalse() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);
        assertEquals(1, changes.size(), "Should produce one field change for the journal field");
        FieldChange fc = changes.getFirst();
        assertEquals(StandardField.JOURNAL, fc.getField());
        assertEquals("Journal of Foo", fc.getOldValue());
        assertEquals("J. Foo", fc.getNewValue());

        assertTrue(entry.getField(AMSField.FJOURNAL).isEmpty(), "Should not set FJournal");
        assertEquals(Optional.of("J. Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalAlreadyAbbreviatedNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);
        assertTrue(changes.isEmpty(), "Expected no changes if already abbreviated");
        assertEquals(Optional.of("J. Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalAndSetFJournal() {
        Abbreviation abbreviation = new Abbreviation("Canadian Journal of Math", "Can. J. Math.");
        Mockito.when(repositoryMock.get("Canadian Journal of Math")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Canadian Journal of Math");

        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);
        assertEquals(2, changes.size());

        FieldChange FJourChange = changes.getFirst();
        assertEquals(AMSField.FJOURNAL, FJourChange.getField());
        assertNull(FJourChange.getOldValue());
        assertEquals("Canadian Journal of Math", FJourChange.getNewValue());

        FieldChange journalChange = changes.get(1);
        assertEquals(StandardField.JOURNAL, journalChange.getField());
        assertEquals("Canadian Journal of Math", journalChange.getOldValue());
        assertEquals("Can. J. Math.", journalChange.getNewValue());

        assertEquals(Optional.of("Can. J. Math."), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Canadian Journal of Math"), entry.getField(AMSField.FJOURNAL));
    }

    @Test
    void abbreviateJournalTitleField() {
        Abbreviation abbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(repositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Physical Review Letters");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);
        assertEquals(1, changes.size());
        FieldChange c = changes.getFirst();
        assertEquals(StandardField.JOURNALTITLE, c.getField());
        assertEquals("Physical Review Letters", c.getOldValue());
        assertEquals("Phys. Rev. Lett.", c.getNewValue());

        assertEquals(Optional.of("Phys. Rev. Lett."), entry.getField(StandardField.JOURNALTITLE));
    }

    @Test
    void testResolveForStringsIsCalled() {
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
        assertEquals(2, changes.size(), "We overwrite the old FJOURNAL if we abbreviate the main field");
        FieldChange FJourChange = changes.getFirst();
        assertEquals("Some old text", FJourChange.getOldValue());
        assertEquals("Journal of Foo", FJourChange.getNewValue());
    }

    @Test
    void braceAroundJournalNameIsIgnoredIfWanted() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "{Journal of Foo}");
        Mockito.when(databaseMock.resolveForStrings("{Journal of Foo}")).thenReturn("Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertEquals(1, changes.size(),
                "Should produce 1 field change if we handle braces around the name");
        assertEquals("J. Foo", entry.getField(StandardField.JOURNAL).orElse("?"));
    }

    @Test
    void trailingWhitespaceIsTrimmedBeforeLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo   ");
        Mockito.when(databaseMock.resolveForStrings("Journal of Foo   ")).thenReturn("Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);
        assertEquals(1, changes.size(),
                "Should produce 1 change if trailing spaces are trimmed for the lookup to succeed");
    }

    @Test
    void databaseResolvesStringsDifferentlyAffectsLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        Mockito.when(databaseMock.resolveForStrings("MACRO_JOURNAL"))
               .thenReturn("Journal of Foo");

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "MACRO_JOURNAL");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertEquals(1, changes.size(),
                "Should produce 1 change since 'MACRO_JOURNAL' is resolved to 'Journal of Foo'");
        FieldChange fc = changes.getFirst();
        assertEquals("MACRO_JOURNAL", fc.getOldValue());
        assertEquals("J. Foo", fc.getNewValue());
    }

    @Test
    void repositoryReturnsSameStringNoChanges() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "Journal of Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);
        assertTrue(changes.isEmpty(),
                "No changes if the 'new' text is identical to the old text");
    }

    @Test
    void repositoryReturnsSameTextButDifferentCase() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "Journal of Foo");
        Mockito.when(repositoryMock.get("JOURNAL OF FOO"))
               .thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "JOURNAL OF FOO");
        Mockito.when(databaseMock.resolveForStrings("JOURNAL OF FOO")).thenReturn("Journal of Foo");
        List<FieldChange> changes = cleanupWithoutFJournal.cleanup(entry);

        assertTrue(changes.isEmpty(), "No changes if 'JOURNAL OF FOO' is same ignoring case");
    }

    @Test
    void journaltitleAndJournalBothRecognized() {
        Abbreviation abbreviationJournal = new Abbreviation("Journal of Bar", "J. Bar");
        Abbreviation abbreviationTitle = new Abbreviation("Review Letters", "Rev. Lett.");

        Mockito.when(repositoryMock.get("Journal of Bar")).thenReturn(Optional.of(abbreviationJournal));
        Mockito.when(repositoryMock.get("Review Letters")).thenReturn(Optional.of(abbreviationTitle));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Bar")
                .withField(StandardField.JOURNALTITLE, "Review Letters");

        List<FieldChange> changes = cleanupWithFJournal.cleanup(entry);

        assertEquals(4, changes.size(),
                "We might expect 4 changes total if we abbreviate both fields and set FJOURNAL for each");
    }
}
