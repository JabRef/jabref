//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS org.jspecify:jspecify:1.0.1
//DEPS org.slf4j:slf4j-api:2.0.20
//DEPS org.slf4j:slf4j-simple:2.0.20
//DEPS tools.jackson.core:jackson-databind:3.3.0

//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/citationstyle/CSLStyleMetadata.java

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CSLStyleMetadata;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/// Generates the bundled CSL catalog without loading the desktop style hierarchy.
@NullMarked
public class CitationStyleCatalogGenerator {
    private static final Path STYLES_ROOT = Path.of("jablib/src/main/resources/csl-styles");
    private static final String CATALOG_PATH = "jablib/build/generated/resources/citation-style-catalog.json";
    private static final String DEFAULT_STYLE = "ieee.csl";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyleCatalogGenerator.class);

    static void main(String[] args) {
        generateCitationStyleCatalog();
    }

    public static void generateCitationStyleCatalog() {
        try {
            Path root = Path.of(".").toAbsolutePath().normalize();
            if (!Files.exists(root.resolve(STYLES_ROOT).resolve(DEFAULT_STYLE)) && (root.getParent() != null)) {
                LOGGER.info("Running from IDE, adjusting path to styles root");
                root = root.getParent();
            }
            Path stylesRoot = root.resolve(STYLES_ROOT);
            if (!Files.exists(stylesRoot.resolve(DEFAULT_STYLE))) {
                LOGGER.error("Could not find any citation style. Tried with {}. Tried in {}. Current directory: {}", DEFAULT_STYLE, root, Path.of(".").toAbsolutePath());
                return;
            }
            List<CatalogStyle> styles = discoverStyles(stylesRoot);
            generateCatalog(styles, stylesRoot, root.resolve(CATALOG_PATH));
        } catch (IOException exception) {
            LOGGER.error("Error generating citation style catalog", exception);
        }
    }

    private static List<CatalogStyle> discoverStyles(Path path) throws IOException {
        List<CatalogStyle> result = new ArrayList<>();
        try (Stream<Path> stream = Files.find(path, 1, (file, _) -> file.toString().endsWith("csl"))) {
            for (Path file : stream.map(Path::toAbsolutePath).toList()) {
                String source;
                try {
                    source = Files.readString(file);
                } catch (IOException exception) {
                    LOGGER.error("Error reading CSL style {}", file, exception);
                    continue;
                }
                CSLStyleMetadata.parseStyleInfo(file.toString(), source).ifPresent(metadata -> result.add(new CatalogStyle(file, metadata)));
            }
        }
        return result;
    }

    private static void generateCatalog(List<CatalogStyle> styles, Path stylesRoot, Path catalogPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> styleInfoList = styles.stream().map(style -> {
            CSLStyleMetadata metadata = style.metadata();
            Map<String, Object> info = new HashMap<>();
            info.put("path", stylesRoot.toAbsolutePath().relativize(style.path()).toString());
            info.put("styleId", metadata.styleId());
            info.put("styleClass", metadata.styleClass());
            info.put("title", metadata.title());
            info.put("shortTitle", metadata.shortTitle());
            info.put("isNumeric", metadata.isNumericStyle());
            info.put("hasBibliography", metadata.hasBibliography());
            info.put("hasBibliographySortOrder", metadata.hasBibliographySortOrder());
            info.put("usesHangingIndent", metadata.usesHangingIndent());
            return info;
        }).toList();
        Files.writeString(catalogPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(styleInfoList));
        LOGGER.info("Generated citation style catalog with {} styles at {}", styles.size(), catalogPath);
    }

    @NullMarked
    private record CatalogStyle(Path path, CSLStyleMetadata metadata) {
    }
}
