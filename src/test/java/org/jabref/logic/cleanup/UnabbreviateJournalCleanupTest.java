package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

class UnabbreviateJournalCleanupTest {

    private UnabbreviateJournalCleanup cleanup;
    private JournalAbbreviationRepository repositoryMock;
    private BibDatabase databaseMock;

    @BeforeEach
    void setUp() {
        databaseMock = Mockito.mock(BibDatabase.class);
        Mockito.when(databaseMock.resolveForStrings(anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        repositoryMock = Mockito.mock(JournalAbbreviationRepository.class);
        Mockito.when(repositoryMock.get(Mockito.anyString())).thenReturn(Optional.empty());

        cleanup = new UnabbreviateJournalCleanup(databaseMock, repositoryMock);
    }

    @Test
    void noJournalFieldsMeansNoChange() {
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = cleanup.cleanup(entry);
        //"Expected no changes when neither field is present"
        assertEquals(List.of(), changes);
    }

    @Test
    void repositoryDoesNotRecognizeJournalNameNoChange() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Unknown Journal");

        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);
        //"No changes expected if the repo can't find an unabbreviation"
        assertEquals(Optional.of("Unknown Journal"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void journalAlreadyUnabbreviatedNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");

        List<FieldChange> changes = cleanup.cleanup(entry);
        assertEquals(List.of(), changes);//"Expected no changes if already unabbreviated"
        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void unabbreviateJournalSuccessful() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");
        FieldChange expectedChange = new FieldChange(entry, StandardField.JOURNAL,"J. Foo", "Journal of Foo");
        List<FieldChange> changes = cleanup.cleanup(entry);
        assertEquals(List.of(expectedChange), changes);
       // assertEquals(1, changes.size()); //Should produce one field change for the journal field
       /* FieldChange fc = changes.getFirst();
        assertEquals(StandardField.JOURNAL, fc.getField());
        assertEquals("J. Foo", fc.getOldValue());
        assertEquals("Journal of Foo", fc.getNewValue());

        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));*/
    }

    @Test
    void restoreFromFJournal() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo")
                .withField(AMSField.FJOURNAL, "Journal of Foo");

        List<FieldChange> changes = cleanup.cleanup(entry);
        assertEquals(2, changes.size());

        FieldChange fjournalChange = changes.getFirst();
        assertEquals(AMSField.FJOURNAL, fjournalChange.getField());
        assertEquals("Journal of Foo", fjournalChange.getOldValue());
        assertEquals("", fjournalChange.getNewValue());

        FieldChange journalChange = changes.get(1);
        assertEquals(StandardField.JOURNAL, journalChange.getField());
        assertEquals("J. Foo", journalChange.getOldValue());
        assertEquals("Journal of Foo", journalChange.getNewValue());

        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
        assertTrue(entry.getField(AMSField.FJOURNAL).isEmpty());
    }

    @Test
    void unabbreviateJournalTitleField() {
        Abbreviation abbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(repositoryMock.isKnownName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("Phys. Rev. Lett.")).thenReturn(true);
        Mockito.when(repositoryMock.get("Phys. Rev. Lett.")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Phys. Rev. Lett.");

        List<FieldChange> changes = cleanup.cleanup(entry);
        assertEquals(1, changes.size());
        FieldChange fc = changes.getFirst();
        assertEquals(StandardField.JOURNALTITLE, fc.getField());
        assertEquals("Phys. Rev. Lett.", fc.getOldValue());
        assertEquals("Physical Review Letters", fc.getNewValue());

        assertEquals(Optional.of("Physical Review Letters"), entry.getField(StandardField.JOURNALTITLE));
    }

    @Test
    void testResolveForStringsIsCalled() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");

        cleanup.cleanup(entry);

        Mockito.verify(databaseMock).resolveForStrings("J. Foo");
    }

    @Test
    void bracesAroundJournalNameAreHandled() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "{J. Foo}");
        Mockito.when(databaseMock.resolveForStrings("{J. Foo}")).thenReturn("J. Foo");
        FieldChange expectedChanges = new FieldChange(entry,StandardField.JOURNAL, "J. Foo", "Journal of Foo");
        //FieldChange(BibEntry entry, Field field, String oldValue, String newValue)
        List<FieldChange> changes = cleanup.cleanup(entry);
        assertEquals(List.of(expectedChanges), changes);
        //assertEquals(1, changes.size(), "Should produce 1 field change if we handle braces around the name");
        assertEquals("Journal of Foo", entry.getField(StandardField.JOURNAL).orElse("?"));
    }

    @Test
    void trailingWhitespaceIsTrimmedBeforeLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, " J. Foo ");
        Mockito.when(databaseMock.resolveForStrings(" J. Foo ")).thenReturn("J. Foo");

        FieldChange expectedChanges = new FieldChange(entry, StandardField.JOURNAL," J. Foo ","Journal of Foo");

        List<FieldChange> changes = cleanup.cleanup(entry);
        //assertEquals(1, changes.size(), "Should produce 1 change if trailing spaces are trimmed for the lookup to succeed");
        assertEquals(List.of(expectedChanges), changes);
//
//        FieldChange fc = changes.getFirst();
//        assertEquals(" J. Foo ", fc.getOldValue());
//        assertEquals("Journal of Foo", fc.getNewValue());

        assertEquals(Optional.of("Journal of Foo"), entry.getField(StandardField.JOURNAL));
    }
}
