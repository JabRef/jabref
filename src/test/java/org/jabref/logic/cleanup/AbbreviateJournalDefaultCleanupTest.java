package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link AbbreviateJournalDefaultCleanup}.
 */
class AbbreviateJournalDefaultCleanupTest {

    private AbbreviateJournalDefaultCleanup cleanupWithoutFjournal;
    private AbbreviateJournalDefaultCleanup cleanupWithFjournal;
    private JournalAbbreviationRepository repositoryMock;

    @BeforeEach
    void setUp() {
        repositoryMock = Mockito.mock(JournalAbbreviationRepository.class);

        Mockito.when(repositoryMock.get(any())).thenReturn(Optional.empty());

        cleanupWithoutFjournal = new AbbreviateJournalDefaultCleanup(repositoryMock, false);
        cleanupWithFjournal = new AbbreviateJournalDefaultCleanup(repositoryMock, true);
    }

    @Test
    void noJournalFieldsMeansNoChange() {
        BibEntry entry = new BibEntry();

        List<FieldChange> changes = cleanupWithoutFjournal.cleanup(entry);
        assertTrue(changes.isEmpty(), "Expected no changes when neither field is present");
    }

    @Test
    void repositoryDoesNotRecognizeJournalNameNoChange() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Some Unknown Journal");

        List<FieldChange> changes = cleanupWithoutFjournal.cleanup(entry);

        assertTrue(changes.isEmpty(), "No changes expected if the repo can't find an abbreviation");
        assertEquals(Optional.of("Some Unknown Journal"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalSuccessfulWithFJournalFalse() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");

        List<FieldChange> changes = cleanupWithoutFjournal.cleanup(entry);
        assertEquals(1, changes.size(), "Should produce one field change for the journal field");
        FieldChange fc = changes.get(0);
        assertEquals(StandardField.JOURNAL, fc.getField());
        assertEquals("Journal of Foo", fc.getOldValue());
        assertEquals("J. Foo", fc.getNewValue());

        assertTrue(entry.getField(AMSField.FJOURNAL).isEmpty(), "Should not set fjournal");
        assertEquals(Optional.of("J. Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalAlreadyAbbreviatedNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");

        List<FieldChange> changes = cleanupWithoutFjournal.cleanup(entry);
        assertTrue(changes.isEmpty(), "Expected no changes if already abbreviated");
        assertEquals(Optional.of("J. Foo"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    void abbreviateJournalAndSetFjournal() {
        Abbreviation abbreviation = new Abbreviation("Canadian Journal of Math", "Can. J. Math.");
        Mockito.when(repositoryMock.get("Canadian Journal of Math")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Canadian Journal of Math");

        List<FieldChange> changes = cleanupWithFjournal.cleanup(entry);
        assertEquals(2, changes.size());

        FieldChange fjourChange = changes.get(0);
        assertEquals(AMSField.FJOURNAL, fjourChange.getField());
        assertNull(fjourChange.getOldValue());
        assertEquals("Canadian Journal of Math", fjourChange.getNewValue());

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

        List<FieldChange> changes = cleanupWithoutFjournal.cleanup(entry);
        assertEquals(1, changes.size());
        FieldChange c = changes.get(0);
        assertEquals(StandardField.JOURNALTITLE, c.getField());
        assertEquals("Physical Review Letters", c.getOldValue());
        assertEquals("Phys. Rev. Lett.", c.getNewValue());

        assertEquals(Optional.of("Phys. Rev. Lett."), entry.getField(StandardField.JOURNALTITLE));
    }
}
