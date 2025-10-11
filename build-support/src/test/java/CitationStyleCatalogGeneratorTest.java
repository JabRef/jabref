// TODO: Integrate in JBang and tests.yml

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.generators.CitationStyleCatalogGenerator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationStyleCatalogGeneratorTest {

    private static final String CATALOG_PATH = "build/generated/resources/citation-style-catalog.json";

    @BeforeEach
    void generateCatalog() {
        CitationStyleCatalogGenerator.generateCitationStyleCatalog();
    }

    @Test
    void catalogGenerationContainsEntries() throws IOException {
        Path catalogPath = Path.of(CATALOG_PATH);

        assertTrue(Files.exists(catalogPath), "Catalog file should exist at " + CATALOG_PATH);

        // Read and parse the catalog
        String catalogContent = Files.readString(catalogPath);
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> styles = mapper.readValue(
                catalogContent,
                new TypeReference<>() {
                });

        assertFalse(styles.isEmpty(), "Catalog should contain at least one citation style");

        Map<String, Object> firstStyle = styles.getFirst();
        assertTrue(firstStyle.containsKey("path"), "Style entry should have a path");
        assertTrue(firstStyle.containsKey("title"), "Style entry should have a title");
        assertTrue(firstStyle.containsKey("shortTitle"), "Style entry should have a short title");
        assertTrue(firstStyle.containsKey("isNumeric"), "Style entry should have isNumeric flag");
        assertTrue(firstStyle.containsKey("hasBibliography"), "Style entry should have hasBibliography flag");
        assertTrue(firstStyle.containsKey("usesHangingIndent"), "Style entry should have usesHangingIndent flag");
    }
}
