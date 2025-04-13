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
        assertTrue(abbreviation.isPresent(), "Should find an abbreviation for 'Test Journal'");
        assertEquals("Test J.", abbreviation.get());
    }

    @Test
    void getNextAbbreviationConvertsAbbreviationToDotlessAbbreviation() {
        Optional<String> abbreviation = repository.getNextAbbreviation("Test J.");
        assertTrue(abbreviation.isPresent(), "Should find dotless abbreviation for 'Test J.'");
        assertEquals("Test J", abbreviation.get());
    }
    
    @Test
    void getNextAbbreviationWrapsBackToFullName() {
        assertEquals("Test J.", repository.getNextAbbreviation("Test Journal").get());
        
        assertEquals("Test J", repository.getNextAbbreviation("Test J.").get());
        
        assertEquals("Test Journal", repository.getNextAbbreviation("Test J").get());
    }
}
