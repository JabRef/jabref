package org.jabref.model.entry.types;

import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BiblatexNonStandardEntryTypeDefinitionsTest {

    @Test
    void allContainsFifteenTypes() {
        assertNotNull(BiblatexNonStandardEntryTypeDefinitions.ALL);
        assertEquals(15, BiblatexNonStandardEntryTypeDefinitions.ALL.size());
    }

    @Test
    void allContainsAllNonStandardTypes() {
        Set<BiblatexNonStandardEntryType> expectedTypes = Set.of(
                BiblatexNonStandardEntryType.Artwork, BiblatexNonStandardEntryType.Audio, BiblatexNonStandardEntryType.Bibnote,
                BiblatexNonStandardEntryType.Commentary, BiblatexNonStandardEntryType.Image, BiblatexNonStandardEntryType.Jurisdiction,
                BiblatexNonStandardEntryType.Legislation, BiblatexNonStandardEntryType.Legal, BiblatexNonStandardEntryType.Letter,
                BiblatexNonStandardEntryType.Movie, BiblatexNonStandardEntryType.Music, BiblatexNonStandardEntryType.Performance,
                BiblatexNonStandardEntryType.Review, BiblatexNonStandardEntryType.Standard, BiblatexNonStandardEntryType.Video
        );

        Set<EntryType> actualTypes = BiblatexNonStandardEntryTypeDefinitions.ALL.stream()
                                                                                .<EntryType>map(BibEntryType::getType)
                                                                                .collect(Collectors.toSet());

        assertEquals(expectedTypes, actualTypes);
    }

    @Test
    void nonStandardTypesHaveSameFieldsAsMisc() {
        BibEntryType miscType = BiblatexEntryTypeDefinitions.getMisc();

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

