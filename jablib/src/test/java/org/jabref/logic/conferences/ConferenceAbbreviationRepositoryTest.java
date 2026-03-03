package org.jabref.logic.conferences;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConferenceAbbreviationRepositoryTest {

    @Test
    void loadsCsvFromStreamAndReturnsAbbreviation() throws Exception {
        // In-memory CSV so we test parsing without depending on external resources
        String csv = "International Conference on Software Engineering,ICSE\n";

        ConferenceAbbreviationRepository repo = new ConferenceAbbreviationRepository(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals("ICSE",
                repo.getAbbreviation("International Conference on Software Engineering").orElseThrow());
    }

    @Test
    void toggleWorksBothWays() throws Exception {
        String csv = "International Conference on Software Engineering,ICSE\n";

        ConferenceAbbreviationRepository repo = new ConferenceAbbreviationRepository(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        // Full -> abbreviation
        assertEquals("ICSE",
                repo.getNextAbbreviation("International Conference on Software Engineering").orElseThrow());

        // Abbreviation -> full
        assertEquals("International Conference on Software Engineering",
                repo.getNextAbbreviation("ICSE").orElseThrow());

        // Unknown -> empty
        assertTrue(repo.getNextAbbreviation("Unknown").isEmpty());
    }

    @Test
    void loadsFromClasspathAndReturnsAbbreviation() throws Exception {
        // Verifies the real acceptance criterion: repository loads the CSV from resources
        ConferenceAbbreviationRepository repo = ConferenceAbbreviationRepository.loadFromClasspath();

        // This line must exist in jablib/src/main/resources/conference-abbreviations.csv
        assertEquals("ICSE",
                repo.getAbbreviation("International Conference on Software Engineering").orElseThrow());
    }
}
