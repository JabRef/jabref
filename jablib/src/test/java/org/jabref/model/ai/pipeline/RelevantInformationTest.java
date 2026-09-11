package org.jabref.model.ai.pipeline;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@NullMarked
class RelevantInformationTest {

    @Test
    void recordComponentValuesArePreserved() {
        RelevantInformation info = new RelevantInformation("Smith2024", 5, "Some excerpt text");

        assertEquals("Smith2024", info.source());
        assertEquals(5, info.pageNumber());
        assertEquals("Some excerpt text", info.text());
    }

    @Test
    void recordAllowsNullPageNumber() {
        RelevantInformation info = new RelevantInformation("Smith2024", null, "Some excerpt text");

        assertEquals("Smith2024", info.source());
        assertNull(info.pageNumber());
        assertEquals("Some excerpt text", info.text());
    }

    @Test
    void recordAllowsNullSource() {
        RelevantInformation info = new RelevantInformation(null, 12, "Some excerpt text");

        assertNull(info.source());
        assertEquals(12, info.pageNumber());
        assertEquals("Some excerpt text", info.text());
    }
}
