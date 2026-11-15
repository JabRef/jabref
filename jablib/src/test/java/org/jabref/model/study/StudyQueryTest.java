package org.jabref.model.study;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudyQueryTest {

    @Test
    void catalogOverrideMatchesCatalogNameCaseInsensitively() {
        StudyQuery query = new StudyQuery("Q1");
        query.getCatalogSpecific().put("acm portal", "ti:Test");

        assertEquals(Optional.of("ti:Test"), query.getCatalogOverride("ACM Portal"));
    }

    @Test
    void blankCatalogOverrideIsAbsent() {
        StudyQuery query = new StudyQuery("Q1");
        query.getCatalogSpecific().put("ACM Portal", " ");

        assertEquals(Optional.empty(), query.getCatalogOverride("ACM Portal"));
    }

    @Test
    void firstNonBlankCatalogOverrideWins() {
        StudyQuery query = new StudyQuery("Q1");
        query.getCatalogSpecific().put("acm portal", "");
        query.getCatalogSpecific().put("ACM Portal", "ti:Test");

        assertEquals(Optional.of("ti:Test"), query.getCatalogOverride("ACM Portal"));
    }

    @Test
    void nullCatalogOverrideIsAbsent() {
        StudyQuery query = new StudyQuery("Q1");
        query.getCatalogSpecific().put("ACM Portal", null);

        assertEquals(Optional.empty(), query.getCatalogOverride("ACM Portal"));
    }
}
