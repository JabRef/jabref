///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 24
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//DEPS org.slf4j:slf4j-api:2.0.13
//DEPS org.slf4j:slf4j-simple:2.0.13

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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            if (!Files.exists(STYLES_ROOT.resolve(DEFAULT_STYLE))) {
                LOGGER.error("Could not find any citation style. Tried with {}.", DEFAULT_STYLE);
                return;
            }

            List<CitationStyle> styles = discoverStyles(STYLES_ROOT);

            generateCatalog(styles);
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

    private static void generateCatalog(List<CitationStyle> styles) throws IOException {
        Path catalogFile = Path.of(CATALOG_PATH);

        // Create a JSON representation of the styles
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> styleInfoList = styles.stream()
                                                        .map(style -> {
                                                            Map<String, Object> info = new HashMap<>();
                                                            Path stylePath = Path.of(style.getFilePath());
                                                            Path relativePath = STYLES_ROOT.toAbsolutePath().relativize(stylePath.toAbsolutePath());
                                                            info.put("path", relativePath.toString());
                                                            info.put("title", style.getTitle());
                                                            info.put("isNumeric", style.isNumericStyle());
                                                            info.put("hasBibliography", style.hasBibliography());
                                                            info.put("usesHangingIndent", style.usesHangingIndent());
                                                            return info;
                                                        })
                                                        .toList();

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(styleInfoList);
        Files.writeString(catalogFile, json);

        LOGGER.info("Generated citation style catalog with {} styles at {}", styles.size(), catalogFile);
    }
}
