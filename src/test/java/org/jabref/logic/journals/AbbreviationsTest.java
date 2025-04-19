package org.jabref.logic.journals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbbreviationsTest {

    private static final Abbreviation DOTTED_JOURNAL = new Abbreviation("Dotted Journal", "Dotted J.");
    private static final Abbreviation TEST_JOURNAL = new Abbreviation("Test Journal", "Test J.");
    
    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JournalAbbreviationRepository();
        
        repository.addCustomAbbreviations(List.of(TEST_JOURNAL, DOTTED_JOURNAL), 
                                         JournalAbbreviationRepository.BUILTIN_LIST_ID, 
                                         true);
    }

    @Test
    void getNextAbbreviationAbbreviatesJournalTitle() {
        Optional<String> abbreviation = repository.getNextAbbreviation("Test Journal");
        assertEquals("Test J.", abbreviation.orElse("WRONG"));
    }

    @Test
    void getNextAbbreviationConvertsAbbreviationToDotlessAbbreviation() {
        Optional<String> abbreviation = repository.getNextAbbreviation("Test J.");
        assertEquals("Test J", abbreviation.orElse("WRONG"));
    }
    
    @Test
    void getNextAbbreviationWrapsBackToFullName() {
        Optional<String> abbreviation1 = repository.getNextAbbreviation("Test Journal");
        assertEquals("Test J.", abbreviation1.orElse("WRONG"));
        
        Optional<String> abbreviation2 = repository.getNextAbbreviation("Test J.");
        assertEquals("Test J", abbreviation2.orElse("WRONG"));
        
        Optional<String> abbreviation3 = repository.getNextAbbreviation("Test J");
        assertEquals("Test Journal", abbreviation3.orElse("WRONG"));
    }

    @Test
    void constructorValidIsoAbbreviation() {
        assertDoesNotThrow(() -> new Abbreviation("Test Entry", "Test. Ent."));
    }

    @Test
    void constructorInvalidMedlineAbbreviation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Abbreviation("Test Entry", null, "TE"));
        assertEquals("ISO/MEDLINE abbreviation is null or empty!", exception.getMessage());
    }

    @Test
    void getShortestUniqueAbbreviationDifferentFromIsoAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Test Entry", "Test. Ent.", "TE");

        assertEquals("TE", abbreviation.getShortestUniqueAbbreviation());
        assertNotEquals("Test. Ent.", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void getNext() {
        Abbreviation abbreviation = new Abbreviation("Test Entry", "Test. Ent.");

        assertEquals("Test. Ent.", abbreviation.getNext("Test Entry"));
        assertEquals("Test Ent", abbreviation.getNext("Test. Ent."));
        assertEquals("Test Entry", abbreviation.getNext("Test Ent"));
    }

    @Test
    void testToString() {
        Abbreviation abbreviation = new Abbreviation("Test Entry", "Test. Ent.");

        assertEquals("Abbreviation{name=Test Entry, iso=Test. Ent., medline=Test Ent, shortest=null}", abbreviation.toString());
    }
}
