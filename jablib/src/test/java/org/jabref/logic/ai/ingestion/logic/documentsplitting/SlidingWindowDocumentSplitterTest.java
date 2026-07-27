package org.jabref.logic.ai.ingestion.logic.documentsplitting;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowDocumentSplitterTest {

    @ParameterizedTest
    @ValueSource(ints = {50, 100, 200})
    void allChunksAreShorterThanOrEqualToChunkSize(int chunkSize) {
        SlidingWindowDocumentSplitter splitter = new SlidingWindowDocumentSplitter(chunkSize, 10);
        String longText = "word ".repeat(500).trim();

        List<String> chunks = splitter.split(longText).toList();

        assertFalse(chunks.isEmpty());
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= chunkSize,
                    "Expected chunk length <= " + chunkSize + " but was " + chunk.length() + ": " + chunk);
        }
    }

    @Test
    void textShorterThanChunkSizeYieldsOneChunk() {
        SlidingWindowDocumentSplitter splitter = new SlidingWindowDocumentSplitter(500, 10);
        String shortText = "This is a short sentence.";

        List<String> chunks = splitter.split(shortText).toList();

        assertEquals(1, chunks.size());
        assertEquals(shortText, chunks.getFirst());
    }

    @Test
    void longTextProducesMultipleChunks() {
        SlidingWindowDocumentSplitter splitter = new SlidingWindowDocumentSplitter(50, 5);
        String longText = "word ".repeat(100).trim();

        List<String> chunks = splitter.split(longText).toList();

        assertTrue(chunks.size() > 1);
    }

    @Test
    void allOriginalWordsAppearInAtLeastOneChunk() {
        SlidingWindowDocumentSplitter splitter = new SlidingWindowDocumentSplitter(60, 10);
        String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron";

        String combined = String.join(" ", splitter.split(text).toList());

        for (String word : text.split(" ")) {
            assertTrue(combined.contains(word), "Word '" + word + "' missing from chunks");
        }
    }
}
