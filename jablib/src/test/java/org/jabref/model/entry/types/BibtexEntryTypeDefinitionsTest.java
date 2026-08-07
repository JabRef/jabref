package org.jabref.model.entry.types;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibtexEntryTypeDefinitionsTest {

    @Test
    void all() {
        assertNotNull(BibtexEntryTypeDefinitions.ALL);
    }

    @Test
    void languageContained() {
        BibEntryType articleEntryType = BiblatexEntryTypeDefinitions.ALL.stream()
                                                                        .filter(type -> type.getType().equals(StandardEntryType.Article))
                                                                        .findFirst()
                                                                        .get();
        assertTrue(articleEntryType.getDetailOptionalFields().contains(StandardField.LANGUAGE));
    }
}
