//JAVA 24+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS org.jspecify:jspecify:1.0.0
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS tools.jackson.core:jackson-databind:3.0.3

//SOURCES ../../../../jablib/src/main/java/org/jabref/architecture/AllowedToUseClassGetResource.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/citationstyle/CSLStyleUtils.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/citationstyle/CitationStyle.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/openoffice/style/OOStyle.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/FileType.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/StandardFileType.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/logic/util/UnknownFileType.java
//SOURCES ../../../../jablib/src/main/java/org/jabref/model/util/OptionalUtil.java

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.citationstyle.CitationStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/// Generates a catalog of CSL styles internally supported by JabRef.
/// The catalog contains the list of styles, along with some pre-computed metadata (e.g. numeric nature).
/// This class is intended to be used for the corresponding build-time task.
///
/// Has to be started in the root of the repository due to <https://github.com/jbangdev/jbang-gradle-plugin/issues/11>
@AllowedToUseClassGetResource("Required for loading internal CSL styles")
public class CitationStyleCatalogGenerator {
    private static final Path STYLES_ROOT = Path.of("jablib/src/main/resources/csl-styles");
    private static final String CATALOG_PATH = "jablib/build/generated/resources/citation-style-catalog.json";
    private static final String DEFAULT_STYLE = "ieee.csl";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyleCatalogGenerator.class);

    public static void main(String[] args) {
        generateCitationStyleCatalog();
    }

    public static void generateCitationStyleCatalog() {
        try {
            // JBang's gradle plugin has a strange path handling. If "application->run" is started from the IDE, the path ends with "jabgui"
            Path root = Path.of(".").toAbsolutePath().normalize();
            String rootFilename = root.getFileName().toString();
            if (!"jabref".equalsIgnoreCase(rootFilename) && rootFilename.startsWith("jab")) {
                LOGGER.info("Running from IDE, adjusting path to styles root");
                root = root.getParent();
            }
            Path stylesRoot = root.resolve(STYLES_ROOT);
            if (!Files.exists(stylesRoot.resolve(DEFAULT_STYLE))) {
                LOGGER.error("Could not find any citation style. Tried with {}. Tried in {}. Current directory: {}", DEFAULT_STYLE, root, Path.of(".").toAbsolutePath());
                return;
            }

            List<CitationStyle> styles = discoverStyles(stylesRoot);

            Path catalogPath = root.resolve(CATALOG_PATH);
            generateCatalog(styles, stylesRoot, catalogPath);
        } catch (IOException e) {
            LOGGER.error("Error generating citation style catalog", e);
        }
    }

    private static List<CitationStyle> discoverStyles(Path path) throws IOException {
        try (Stream<Path> stream = Files.find(path, 1, (file, _) -> file.toString().endsWith("csl"))) {
            return stream.map(Path::toAbsolutePath)
                         .map(Path::toString)
                         .map(CSLStyleUtils::createCitationStyleFromFile)
                         .flatMap(Optional::stream)
                         .toList();
        }
    }

    private static void generateCatalog(List<CitationStyle> styles, Path stylesRoot, Path catalogPath) throws IOException {
        // Create a JSON representation of the styles
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> styleInfoList = styles.stream()
                                                        .map(style -> {
                                                            Map<String, Object> info = new HashMap<>();
                                                            Path stylePath = Path.of(style.getFilePath());
                                                            Path relativePath = stylesRoot.toAbsolutePath().relativize(stylePath.toAbsolutePath());
                                                            info.put("path", relativePath.toString());
                                                            info.put("title", style.getTitle());
                                                            info.put("shortTitle", style.getShortTitle());
                                                            info.put("isNumeric", style.isNumericStyle());
                                                            info.put("hasBibliography", style.hasBibliography());
                                                            info.put("usesHangingIndent", style.usesHangingIndent());
                                                            return info;
                                                        })
                                                        .toList();

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(styleInfoList);
        Files.writeString(catalogPath, json);

        LOGGER.info("Generated citation style catalog with {} styles at {}", styles.size(), catalogPath);
    }
}
