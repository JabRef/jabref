package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseClassGetResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a catalog of CSL styles internally supported by JabRef.
 * The catalog contains the list of styles, along with some pre-computed metadata (e.g. numeric nature).
 * This class is intended to be used for the corresponding build-time task.
 */
@AllowedToUseClassGetResource("Required for loading internal CSL styles")
public class CitationStyleCatalogGenerator {
    private static final String STYLES_ROOT = "/csl-styles";
    private static final String CATALOG_PATH = "src/main/resources/citation-style-catalog.json";
    private static final String DEFAULT_STYLE = "ieee.csl";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyleCatalogGenerator.class);

    public static void main(String[] args) {
        generateCitationStyleCatalog();
    }

    public static void generateCitationStyleCatalog() {
        try {
            URL url = CitationStyleCatalogGenerator.class.getResource(STYLES_ROOT + "/" + DEFAULT_STYLE);
            if (url == null) {
                LOGGER.error("Could not find any citation style. Tried with {}.", DEFAULT_STYLE);
                return;
            }

            URI uri = url.toURI();
            Path stylesDirectory = Path.of(uri).getParent();
            List<CitationStyle> styles = discoverStyles(stylesDirectory);

            generateCatalog(styles);
        } catch (Exception e) {
            LOGGER.error("Error generating citation style catalog", e);
        }
    }

    private static List<CitationStyle> discoverStyles(Path path) throws IOException {
        try (Stream<Path> stream = Files.find(path, 1, (file, attr) -> file.toString().endsWith("csl"))) {
            return stream.map(Path::getFileName)
                         .map(Path::toString)
                         .map(CSLStyleUtils::createCitationStyleFromFile)
                         .flatMap(Optional::stream)
                         .toList();
        }
    }

    private static void generateCatalog(List<CitationStyle> styles) throws IOException {
        Path catalogFile = Path.of(CATALOG_PATH);

        // Create a JSON representation of the styles
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> styleInfoList = styles.stream()
                                                        .map(style -> {
                                                            Map<String, Object> info = new HashMap<>();
                                                            info.put("path", style.getFilePath());
                                                            info.put("title", style.getTitle());
                                                            info.put("isNumeric", style.isNumericStyle());
                                                            return info;
                                                        })
                                                        .toList();

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(styleInfoList);
        Files.writeString(catalogFile, json);

        LOGGER.info("Generated citation style catalog with {} styles at {}", styles.size(), catalogFile);
    }
}
