package org.jabref.http.dto;

import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibEntryDTOTest {

    private FieldPreferences fieldPreferences;
    private BibEntryTypesManager bibEntryTypesManager;

    @BeforeEach
    void setUp() {
        fieldPreferences = new FieldPreferences(false, List.of(), List.of());
        bibEntryTypesManager = new BibEntryTypesManager();
    }

    @Test
    void constructorPopulatesCitationKeyAndBibtexFromBibEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Author2023")
                .withField(StandardField.AUTHOR, "Jane Doe")
                .withField(StandardField.TITLE, "A Great Paper")
                .withField(StandardField.YEAR, "2023");
        entry.setChanged(true);

        BibEntryDTO dto = new BibEntryDTO(entry, BibDatabaseMode.BIBTEX, fieldPreferences, bibEntryTypesManager);

        assertEquals("Author2023", dto.citationKey());
        assertTrue(dto.bibtex().contains("Author2023"));
        assertTrue(dto.bibtex().contains("Jane Doe"));
        assertTrue(dto.bibtex().contains("A Great Paper"));
    }

    @Test
    void constructorSetsEmptyCitationKeyWhenMissing() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "No Key Paper");
        entry.setChanged(true);

        BibEntryDTO dto = new BibEntryDTO(entry, BibDatabaseMode.BIBTEX, fieldPreferences, bibEntryTypesManager);

        assertEquals("", dto.citationKey());
    }

    @Test
    void compareToOrdersBySharedIdThenVersion() {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article).withCitationKey("a");
        entry1.getSharedBibEntryData().setSharedID(1);
        entry1.setChanged(true);
        BibEntry entry2 = new BibEntry(StandardEntryType.Article).withCitationKey("b");
        entry2.getSharedBibEntryData().setSharedID(5);
        entry2.setChanged(true);
        BibEntry entry3 = new BibEntry(StandardEntryType.Article).withCitationKey("c");
        entry3.getSharedBibEntryData().setSharedID(5);
        entry3.getSharedBibEntryData().setVersion(9);
        entry3.setChanged(true);

        BibEntryDTO dto1 = new BibEntryDTO(entry1, BibDatabaseMode.BIBTEX, fieldPreferences, bibEntryTypesManager);
        BibEntryDTO dto2 = new BibEntryDTO(entry2, BibDatabaseMode.BIBTEX, fieldPreferences, bibEntryTypesManager);
        BibEntryDTO dto3 = new BibEntryDTO(entry3, BibDatabaseMode.BIBTEX, fieldPreferences, bibEntryTypesManager);

        assertTrue(dto1.compareTo(dto2) < 0, "smaller sharedID should sort first");
        assertTrue(dto2.compareTo(dto1) > 0, "larger sharedID should sort after");
        assertTrue(dto2.compareTo(dto3) < 0, "same sharedID, smaller version should sort first");
        assertEquals(0, dto1.compareTo(dto1), "same DTO should be equal to itself");
    }

    @Test
    void toStringIncludesAllRecordComponents() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("myKey");
        entry.getSharedBibEntryData().setSharedID(42);
        entry.setCommentsBeforeEntry("% a comment");
        entry.setChanged(true);

        BibEntryDTO dto = new BibEntryDTO(entry, BibDatabaseMode.BIBTEX, fieldPreferences, bibEntryTypesManager);

        String str = dto.toString();
        assertTrue(str.contains("sharingMetadata"));
        assertTrue(str.contains("citationkey=myKey"));
        assertTrue(str.contains("bibtex"));
        assertTrue(str.contains("userComments"));
    }
}
