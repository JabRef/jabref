package org.jabref.model.entry;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordTest {

    @Test
    void getPathFromRootAsStringForSimpleChain() throws Exception {
        Keyword keywordChain = Keyword.of("A", "B", "C");
        assertEquals("A > B", keywordChain.getChild().get().getPathFromRootAsString('>'));
    }

    @Test
    void getAllSubchainsAsStringForSimpleChain() throws Exception {
        Keyword keywordChain = Keyword.of("A", "B", "C");
        Set<String> expected = new HashSet<>();
        expected.add("A");
        expected.add("A > B");
        expected.add("A > B > C");

        assertEquals(expected, keywordChain.getAllSubchainsAsString('>'));
    }
}
