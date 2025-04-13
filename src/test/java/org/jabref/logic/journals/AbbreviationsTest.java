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
}
