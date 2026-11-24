package org.jabref.logic.ai.embedding;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbeddingInputPrefixesTest {

    @ParameterizedTest
    @CsvSource({
            "intfloat/e5-small-v2, true",
            "intfloat/multilingual-e5-small, true",
            "intfloat/multilingual-e5-large-instruct, false",
            "sentence-transformers/all-MiniLM-L12-v2, false",
            "BAAI/bge-small-en-v1.5, false"
    })
    void isE5(String modelName, boolean expected) {
        assertEquals(expected, EmbeddingInputPrefixes.isE5(modelName));
    }
}
