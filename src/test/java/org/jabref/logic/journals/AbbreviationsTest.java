package org.jabref.logic.journals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbbreviationsTest {

    private JournalAbbreviationRepository repository;
    
    private static final Abbreviation TEST_JOURNAL = new Abbreviation("Test Journal", "Test J.");
    private static final Abbreviation DOTTED_JOURNAL = new Abbreviation("Dotted Journal", "Dotted J.");

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
        assertTrue(abbreviation.isPresent());
        assertEquals("Test J.", abbreviation.get());
    }

    @Test
    void getNextAbbreviationConvertsAbbreviationToDotlessAbbreviation() {
        Optional<String> abbreviation = repository.getNextAbbreviation("Test J.");
        assertTrue(abbreviation.isPresent());
        assertEquals("Test J", abbreviation.get());
    }
    
    @Test
    void getNextAbbreviationWrapsBackToFullName() {
        Optional<String> abbreviation1 = repository.getNextAbbreviation("Test Journal");
        assertTrue(abbreviation1.isPresent());
        assertEquals("Test J.", abbreviation1.get());
        
        Optional<String> abbreviation2 = repository.getNextAbbreviation("Test J.");
        assertTrue(abbreviation2.isPresent());
        assertEquals("Test J", abbreviation2.get());
        
        Optional<String> abbreviation3 = repository.getNextAbbreviation("Test J");
        assertTrue(abbreviation3.isPresent());
        assertEquals("Test Journal", abbreviation3.get());
    }
}
