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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

public class AbbreviateJournalCleanupTest {
    private AbbreviateJournalCleanup cleanupWithoutFJournal;
    private AbbreviateJournalCleanup cleanupWithFJournal;
    private AbbreviateJournalCleanup cleanupWithoutDatabaseOrFJournal;
    private AbbreviateJournalCleanup cleanupWithoutDatabase;

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
        cleanupWithoutDatabaseOrFJournal = new AbbreviateJournalCleanup(repositoryMock, AbbreviationType.DEFAULT, false);
        cleanupWithoutDatabase = new AbbreviateJournalCleanup(repositoryMock, AbbreviationType.DEFAULT, true);
    }

    @ParameterizedTest
    @CsvSource({
            "true, true",
            "true, false",
            "false, true",
            "false, false"
    })
    void repositoryDoesNotRecognizeJournalNameNoChange(boolean withDatabase, boolean useFJournalField) {
        AbbreviateJournalCleanup cleanup = withDatabase
                                           ? new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DEFAULT, useFJournalField)
                                           : new AbbreviateJournalCleanup(repositoryMock, AbbreviationType.DEFAULT, useFJournalField);
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Some Unknown Journal");

        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Some Unknown Journal");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalSuccessfulWithFJournalFalse(boolean withDatabase) {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        AbbreviateJournalCleanup cleanup = withDatabase ? cleanupWithoutFJournal : cleanupWithoutDatabaseOrFJournal;
        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Journal of Foo", "J. Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalAlreadyAbbreviatedNoChange(boolean withDatabase) {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "J. Foo");
        AbbreviateJournalCleanup cleanup = withDatabase ? cleanupWithoutFJournal : cleanupWithoutDatabaseOrFJournal;
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalAndSetFJournal(boolean withDatabase) {
        Abbreviation abbreviation = new Abbreviation("Canadian Journal of Math", "Can. J. Math.");
        Mockito.when(repositoryMock.get("Canadian Journal of Math")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Canadian Journal of Math");
        AbbreviateJournalCleanup cleanup = withDatabase ? cleanupWithFJournal : cleanupWithoutDatabase;
        List<FieldChange> changes = cleanup.cleanup(entry);

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalTitleField(boolean withDatabase) {
        Abbreviation abbreviation = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.");
        Mockito.when(repositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Physical Review Letters");
        AbbreviateJournalCleanup cleanup = withDatabase ? cleanupWithoutFJournal : cleanupWithoutDatabaseOrFJournal;
        List<FieldChange> changes = cleanup.cleanup(entry);

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
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        cleanupWithoutFJournal.cleanup(entry);

        Mockito.verify(databaseMock).resolveForStrings("Journal of Foo");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void oldFJournalIsOverwrittenWhenUseFJournalIsTrue(boolean withDatabase) {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo")
                .withField(AMSField.FJOURNAL, "Some old text");

        AbbreviateJournalCleanup cleanup = withDatabase ? cleanupWithFJournal : cleanupWithoutDatabase;
        List<FieldChange> changes = cleanup.cleanup(entry);

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
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

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
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

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
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));
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
    void resolveStringsWithoutDatabaseNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));
        Mockito.when(databaseMock.resolveForStrings("MACRO_JOURNAL")).thenReturn("Journal of Foo");

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "MACRO_JOURNAL");
        List<FieldChange> changes = cleanupWithoutDatabaseOrFJournal.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "MACRO_JOURNAL");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void repositoryReturnsSameStringNoChanges(boolean withDatabase) {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "Journal of Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        AbbreviateJournalCleanup cleanup = withDatabase ? cleanupWithFJournal : cleanupWithoutDatabase;
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void journalTitleAndJournalBothRecognized(boolean withDatabase) {
        Abbreviation abbreviationJournal = new Abbreviation("Journal of Bar", "J. Bar");
        Abbreviation abbreviationTitle = new Abbreviation("Review Letters", "Rev. Lett.");

        Mockito.when(repositoryMock.get("Journal of Bar")).thenReturn(Optional.of(abbreviationJournal));
        Mockito.when(repositoryMock.get("Review Letters")).thenReturn(Optional.of(abbreviationTitle));

        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Bar")
                .withField(StandardField.JOURNALTITLE, "Review Letters");

        AbbreviateJournalCleanup testCleanup = withDatabase ? cleanupWithFJournal : cleanupWithoutDatabase;
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalDotlessNoFjournal(boolean withDatabase) {
        Abbreviation abbr = new Abbreviation("Long Name", "L. N.", "LN");
        Mockito.when(repositoryMock.get("Long Name")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = withDatabase
                                                  ? new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DOTLESS, false)
                                                  : new AbbreviateJournalCleanup(repositoryMock, AbbreviationType.DOTLESS, false);

        List<FieldChange> changes = dotlessCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Long Name", "L N")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "L N");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalDotlessWithFjournal(boolean withDatabase) {
        Abbreviation abbr = new Abbreviation("Long Name", "L. N.", "LN");
        Mockito.when(repositoryMock.get("Long Name")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Long Name");
        AbbreviateJournalCleanup dotlessCleanup = withDatabase
                                                  ? new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.DOTLESS, true)
                                                  : new AbbreviateJournalCleanup(repositoryMock, AbbreviationType.DOTLESS, true);

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalShortestUniqueNoFjournal(boolean withDatabase) {
        Abbreviation abbr = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.", "PRL");
        Mockito.when(repositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = withDatabase
                                                   ? new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.SHORTEST_UNIQUE, false)
                                                   : new AbbreviateJournalCleanup(repositoryMock, AbbreviationType.SHORTEST_UNIQUE, false);

        List<FieldChange> changes = shortestCleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Physical Review Letters", "PRL")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "PRL");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void abbreviateJournalShortestUniqueWithFjournal(boolean withDatabase) {
        Abbreviation abbr = new Abbreviation("Physical Review Letters", "Phys. Rev. Lett.", "PRL");
        Mockito.when(repositoryMock.get("Physical Review Letters")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Physical Review Letters");
        AbbreviateJournalCleanup shortestCleanup = withDatabase
                                                   ? new AbbreviateJournalCleanup(databaseMock, repositoryMock, AbbreviationType.SHORTEST_UNIQUE, true)
                                                   : new AbbreviateJournalCleanup(repositoryMock, AbbreviationType.SHORTEST_UNIQUE, true);

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ampersandStaysEscaped(boolean withDatabase) {
        Abbreviation abbr = new Abbreviation("Aachen & Berlin", "A & B", "AB");
        Mockito.when(repositoryMock.get("A & B")).thenReturn(Optional.of(abbr));
        Mockito.when(repositoryMock.get("A \\& B")).thenReturn(Optional.of(abbr));
        Mockito.when(repositoryMock.get("Aachen & Berlin")).thenReturn(Optional.of(abbr));
        Mockito.when(repositoryMock.get("Aachen \\& Berlin")).thenReturn(Optional.of(abbr));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "A \\& B");
        BibEntry expected = new BibEntry(entry).withField(AMSField.FJOURNAL, "Aachen \\& Berlin");
        UnabbreviateJournalCleanup unabbreviateCleanup = withDatabase
                                                         ? new UnabbreviateJournalCleanup(databaseMock, repositoryMock)
                                                         : new UnabbreviateJournalCleanup(repositoryMock);
        AbbreviateJournalCleanup abbreviateCleanup = withDatabase ? cleanupWithFJournal : cleanupWithoutDatabase;
        unabbreviateCleanup.cleanup(entry);
        abbreviateCleanup.cleanup(entry);
        assertEquals(expected, entry);
    }
}
