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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationStyleCatalogGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyleCatalogGenerator.class);
    private static final String STYLES_ROOT = "/csl-styles";

    public static void main(String[] args) {
        generateCitationStyleCatalog();
    }

    public static void generateCitationStyleCatalog() {
        try {
            // Find the resources directory in your build environment
            URL url = CitationStyleCatalogGenerator.class.getResource(STYLES_ROOT + CitationStyle.DEFAULT);
            if (url == null) {
                LOGGER.error("Could not find citation styles directory");
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
                         .map(CitationStyle::createCitationStyleFromFile)
                         .filter(Optional::isPresent)
                         .map(Optional::get)
                         .collect(Collectors.toList());
        }
    }

    private static void generateCatalog(List<CitationStyle> styles) throws IOException {
        Path catalogFile = Path.of("src/main/resources/citation-style-catalog.json");

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
                                                        .collect(Collectors.toList());

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(styleInfoList);
        Files.writeString(catalogFile, json);

        LOGGER.info("Generated citation style catalog with {} styles at {}", styles.size(), catalogFile);
    }
}
