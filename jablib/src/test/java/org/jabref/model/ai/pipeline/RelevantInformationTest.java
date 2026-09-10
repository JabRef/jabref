package org.jabref.model.ai.pipeline;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@NullMarked
class RelevantInformationTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void constructorWithPageNumberSetsAllFields() {
        RelevantInformation info = new RelevantInformation("Smith2024", 5, "Some excerpt text");

        assertEquals("Smith2024", info.source());
        assertEquals("Smith2024", info.citationKey());
        assertEquals(5, info.pageNumber());
        assertEquals("Some excerpt text", info.text());
    }

    @Test
    void constructorWithoutPageNumberSetsPageNumberToNull() {
        RelevantInformation info = new RelevantInformation("Smith2024", "Some excerpt text");

        assertEquals("Smith2024", info.source());
        assertEquals("Smith2024", info.citationKey());
        assertNull(info.pageNumber());
        assertEquals("Some excerpt text", info.text());
    }

    @Test
    void constructorWithAlternativeOrderSetsAllFields() {
        RelevantInformation info = new RelevantInformation("Smith2024", "Some excerpt text", 42);

        assertEquals("Smith2024", info.source());
        assertEquals(42, info.pageNumber());
        assertEquals("Some excerpt text", info.text());
    }

    @Test
    void jsonSerializationAndDeserializationPreservesPageNumber() throws Exception {
        RelevantInformation original = new RelevantInformation("Einstein1905", 17, "Relativity text");

        String json = jsonMapper.writeValueAsString(original);
        RelevantInformation deserialized = jsonMapper.readValue(json, RelevantInformation.class);

        assertEquals(original, deserialized);
        assertEquals(17, deserialized.pageNumber());
    }

    @Test
    void jsonDeserializationWithoutPageNumberSetsItToNull() throws Exception {
        String json = "{\"source\":\"Bohr1913\",\"text\":\"Atomic theory\"}";

        RelevantInformation deserialized = jsonMapper.readValue(json, RelevantInformation.class);

        assertEquals("Bohr1913", deserialized.source());
        assertNull(deserialized.pageNumber());
        assertEquals("Atomic theory", deserialized.text());
    }
}
