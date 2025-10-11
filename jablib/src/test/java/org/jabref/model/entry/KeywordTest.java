package org.jabref.model.entry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordTest {

    @Test
    void getPathFromRootAsStringForSimpleChain() {
        Keyword keywordChain = Keyword.of(List.of("A", "B", "C"));
        assertEquals("A > B", keywordChain.getChild().get().getPathFromRootAsString('>'));
    }

    @Test
    void getAllSubchainsAsStringForSimpleChain() {
        Keyword keywordChain = Keyword.of(List.of("A", "B", "C"));
        Set<String> expected = new HashSet<>();
        expected.add("A");
        expected.add("A > B");
        expected.add("A > B > C");

        assertEquals(expected, keywordChain.getAllSubchainsAsString('>'));
    }

    @Test
    void ofHierarchicalCreatesCorrectKeywordChain() {
        Keyword keywordChain = Keyword.ofHierarchical("A > B > C");
        assertEquals("A", keywordChain.get());
        assertTrue(keywordChain.getAllSubchainsAsString('>').contains("A > B > C"));
    }

    @Test
    void ofHierarchicalCreatesKeywordWithoutDelimiter() {
        Keyword keyword = Keyword.ofHierarchical("SingleKeyword");
        assertEquals("SingleKeyword", keyword.get());
    }

    @Test
    void ofHierarchicalEmptyString() {
        Keyword keyword = Keyword.ofHierarchical("");
        assertEquals("", keyword.get());
    }
}
