package org.jabref.model.entry;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordTest {

    @Test
    void getPathFromRootAsStringForSimpleChain() {
        Keyword keywordChain = Keyword.of("A", "B", "C");
        assertEquals("A > B", keywordChain.getChild().get().getPathFromRootAsString('>'));
    }

    @Test
    void getAllSubchainsAsStringForSimpleChain() {
        Keyword keywordChain = Keyword.of("A", "B", "C");
        Set<String> expected = new HashSet<>();
        expected.add("A");
        expected.add("A > B");
        expected.add("A > B > C");

        assertEquals(expected, keywordChain.getAllSubchainsAsString('>'));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Keyword > Keyword",
            "Keyword \\> Keyword"
    })
    void getSubchainAsString(String input) {
        Keyword keyword = new Keyword(input);
        String result = keyword.toString(); // wraps Keyword#getSubchainAsString
        assertEquals(input, result);
    }
}
