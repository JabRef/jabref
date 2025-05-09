package org.jabref.model.entry.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BiblatexEntryTypeDefinitionsTest {

    @Test
    void all() {
        assertNotNull(BiblatexEntryTypeDefinitions.ALL);
    }
}
