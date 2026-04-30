package org.jabref.logic.journals;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class JournalAbbreviationLoaderLocalTest {

    @Test
    void returnsRepositoryWhenExternalListsAreEmpty() {
        JournalAbbreviationPreferences preferences =
                new JournalAbbreviationPreferences(Collections.emptyList(), false);

        JournalAbbreviationRepository repository =
                JournalAbbreviationLoader.loadRepository(preferences);

        assertNotNull(repository);
    }
}
