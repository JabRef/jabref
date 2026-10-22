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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class UnabbreviateJournalCleanupTest {
    private UnabbreviateJournalCleanup cleanupWithDatabase;
    private UnabbreviateJournalCleanup cleanupWithoutDatabase;
    private JournalAbbreviationRepository repositoryMock;
    private BibDatabase databaseMock;

    @BeforeEach
    void setUp() {
        databaseMock = Mockito.mock(BibDatabase.class);
        Mockito.when(databaseMock.resolveForStrings(anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        repositoryMock = Mockito.mock(JournalAbbreviationRepository.class);
        Mockito.when(repositoryMock.get(anyString())).thenReturn(Optional.empty());

        cleanupWithDatabase = new UnabbreviateJournalCleanup(databaseMock, repositoryMock);
        cleanupWithoutDatabase = new UnabbreviateJournalCleanup(repositoryMock);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void noJournalFieldsMeansNoChange(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
        BibEntry entry = new BibEntry();
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void repositoryDoesNotRecognizeJournalNameNoChange(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Unknown Journal");
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Unknown Journal");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void journalAlreadyUnabbreviatedNoChange(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Journal of Foo");
        List<FieldChange> changes = cleanup.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unabbreviateJournalSuccessful(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
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

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unabbreviateJournalTitleSuccessful(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
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

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unabbreviateBothJournalTitleAndJournalSuccessful(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
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

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo")
                .withField(StandardField.JOURNALTITLE, "Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void restoreUnabbreviatedJournalTitleFromFJournal(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void OnEmptyFJournalFieldShouldUseJournalAbbreviationRepository(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "J. Foo")
                .withField(AMSField.FJOURNAL, "");

        Abbreviation abbreviationJournal = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviationJournal));

        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "J. Foo", "Journal of Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);

        verify(repositoryMock, atLeastOnce()).get("J. Foo");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unabbreviateJournalTitleField(boolean withDatabase) {
        UnabbreviateJournalCleanup cleanup = withDatabase ? cleanupWithDatabase : cleanupWithoutDatabase;
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

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "Physical Review Letters");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void resolveStringsWithoutDatabaseNoChange() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(databaseMock.resolveForStrings("MACRO_JOURNAL")).thenReturn("J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "MACRO_JOURNAL");
        List<FieldChange> changes = cleanupWithoutDatabase.cleanup(entry);

        assertEquals(List.of(), changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "MACRO_JOURNAL");
        assertEquals(expectedEntry, entry);
    }

    @Test
    void trailingWhitespaceIsTrimmedBeforeLookup() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        Mockito.when(repositoryMock.isKnownName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.isAbbreviatedName("J. Foo")).thenReturn(true);
        Mockito.when(repositoryMock.get("J. Foo")).thenReturn(Optional.of(abbreviation));

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, " J. Foo ");
        Mockito.when(databaseMock.resolveForStrings(" J. Foo ")).thenReturn("J. Foo");

        List<FieldChange> changes = cleanupWithDatabase.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, " J. Foo ", "Journal of Foo")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Journal of Foo");
        assertEquals(expectedEntry, entry);
    }
}
