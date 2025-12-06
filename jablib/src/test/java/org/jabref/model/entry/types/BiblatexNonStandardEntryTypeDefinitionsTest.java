package org.jabref.model.entry.types;

import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexNonStandardEntryTypeDefinitions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiblatexNonStandardEntryTypeDefinitionsTest {

    @Test
    void allContainsFifteenTypes() {
        assertNotNull(BiblatexNonStandardEntryTypeDefinitions.ALL);
        assertEquals(15, BiblatexNonStandardEntryTypeDefinitions.ALL.size());
    }

    @Test
    void allContainsAllNonStandardTypes() {
        Set<StandardEntryType> expectedTypes = Set.of(
                StandardEntryType.Artwork, StandardEntryType.Audio, StandardEntryType.Bibnote,
                StandardEntryType.Commentary, StandardEntryType.Image, StandardEntryType.Jurisdiction,
                StandardEntryType.Legislation, StandardEntryType.Legal, StandardEntryType.Letter,
                StandardEntryType.Movie, StandardEntryType.Music, StandardEntryType.Performance,
                StandardEntryType.Review, StandardEntryType.Standard, StandardEntryType.Video
        );

        Set<EntryType> actualTypes = BiblatexNonStandardEntryTypeDefinitions.ALL.stream()
                                                                                .map(BibEntryType::getType)
                                                                                .collect(Collectors.toSet());

        assertEquals(expectedTypes, actualTypes);
    }

    @Test
    void nonStandardTypesHaveSameFieldsAsMisc() {
        BibEntryType miscType = BiblatexEntryTypeDefinitions.MISC;

        for (BibEntryType nonStandardType : BiblatexNonStandardEntryTypeDefinitions.ALL) {
            // Check required fields
            assertEquals(miscType.getRequiredFields(), nonStandardType.getRequiredFields(),
                    "Type " + nonStandardType.getType().getName() + " should have same required fields as MISC");

            // Check all fields
            assertEquals(miscType.getAllBibFields(), nonStandardType.getAllBibFields(),
                    "Type " + nonStandardType.getType().getName() + " should have same fields as MISC");
        }
    }
}

