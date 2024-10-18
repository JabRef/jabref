package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.util.StandardFileType;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a CitationStyle. Stores its name, the file path and the style itself
 */
@AllowedToUseClassGetResource("org.jabref.logic.citationstyle.CitationStyle.discoverCitationStyles reads the whole path to discover all available styles. Should be converted to a build-time job.")
public class CitationStyle implements OOStyle {

    public static final String DEFAULT = "/ieee.csl";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyle.class);
    private static final String STYLES_ROOT = "/csl-styles";
    private static final List<CitationStyle> STYLES = new ArrayList<>();
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private final String filePath;
    private final String title;
    private final boolean isNumericStyle;
    private final String source;

    private CitationStyle(final String filename, final String title, final boolean isNumericStyle, final String source) {
        this.filePath = Objects.requireNonNull(filename);
        this.title = Objects.requireNonNull(title);
        this.isNumericStyle = isNumericStyle;
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Creates an CitationStyle instance out of the style string
     */
    private static Optional<CitationStyle> createCitationStyleFromSource(final InputStream source, final String filename) {
        try {
            String content = new String(source.readAllBytes());

            Optional<StyleInfo> styleInfo = parseStyleInfo(filename, content);
            if (styleInfo.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new CitationStyle(filename, styleInfo.get().title(), styleInfo.get().isNumericStyle(), content));
        } catch (IOException e) {
            LOGGER.error("Error while parsing source", e);
            return Optional.empty();
        }
    }

    public record StyleInfo(String title, boolean isNumericStyle) {
    }

    @VisibleForTesting
    static Optional<StyleInfo> parseStyleInfo(String filename, String content) {
        FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);

        try {
            XMLStreamReader reader = FACTORY.createXMLStreamReader(new StringReader(content));

            boolean inInfo = false;
            boolean hasBibliography = false;
            String title = "";
            boolean isNumericStyle = false;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();

                    switch (elementName) {
                        case "bibliography" -> hasBibliography = true;
                        case "info" -> inInfo = true;
                        case "title" -> {
                            if (inInfo) {
                                title = reader.getElementText();
                            }
                        }
                        case "category" -> {
                            String citationFormat = reader.getAttributeValue(null, "citation-format");
                            if (citationFormat != null) {
                                isNumericStyle = "numeric".equals(citationFormat);
                            }
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("info".equals(reader.getLocalName())) {
                        inInfo = false;
                    }
                }
            }

            if (hasBibliography && title != null) {
                return Optional.of(new StyleInfo(title, isNumericStyle));
            } else {
                LOGGER.debug("No valid title or bibliography found for file {}", filename);
                return Optional.empty();
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Error parsing XML for file {}: {}", filename, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static String stripInvalidProlog(String source) {
        int startIndex = source.indexOf("<");
        if (startIndex > 0) {
            return source.substring(startIndex);
        } else {
            return source;
        }
    }

    /**
     * Loads the CitationStyle from the given file
     */
    public static Optional<CitationStyle> createCitationStyleFromFile(final String styleFile) {
        if (!isCitationStyleFile(styleFile)) {
            LOGGER.error("Can only load style files: {}", styleFile);
            return Optional.empty();
        }

        String internalFile = STYLES_ROOT + (styleFile.startsWith("/") ? "" : "/") + styleFile;
        Path internalFilePath = Path.of(internalFile);
        boolean isExternalFile = Files.exists(internalFilePath);
        try (InputStream inputStream = isExternalFile ? Files.newInputStream(internalFilePath) : CitationStyle.class.getResourceAsStream(internalFile)) {
            if (inputStream == null) {
                LOGGER.error("Could not find file: {}", styleFile);
                return Optional.empty();
            }
            return createCitationStyleFromSource(inputStream, styleFile);
        } catch (NoSuchFileException e) {
            LOGGER.error("Could not find file: {}", styleFile, e);
        } catch (IOException e) {
            LOGGER.error("Error reading source file", e);
        }
        return Optional.empty();
    }

    /**
     * Provides the default citation style which is currently IEEE
     *
     * @return default citation style
     */
    public static CitationStyle getDefault() {
        return createCitationStyleFromFile(DEFAULT).orElse(new CitationStyle("", "Empty", false, ""));
    }

    /**
     * Provides the citation styles that come with JabRef.
     *
     * @return list of available citation styles
     */
    public static List<CitationStyle> discoverCitationStyles() {
        if (!STYLES.isEmpty()) {
            return STYLES;
        }

        // TODO: The list of files should be determined at build time (instead of the dynamic method in discoverCitationStylesInPath(path))

        URL url = CitationStyle.class.getResource(STYLES_ROOT + DEFAULT);
        if (url == null) {
            LOGGER.error("Could not find any citation style. Tried with {}.", DEFAULT);
            return Collections.emptyList();
        }

        try {
            URI uri = url.toURI();
            Path path = Path.of(uri).getParent();
            STYLES.addAll(discoverCitationStylesInPath(path));

            return STYLES;
        } catch (URISyntaxException
                 | IOException e) {
            LOGGER.error("something went wrong while searching available CitationStyles", e);
            return Collections.emptyList();
        }
    }

    private static List<CitationStyle> discoverCitationStylesInPath(Path path) throws IOException {
        try (Stream<Path> stream = Files.find(path, 1, (file, attr) -> file.toString().endsWith("csl"))) {
            return stream.map(Path::getFileName)
                         .map(Path::toString)
                         .map(CitationStyle::createCitationStyleFromFile)
                         .filter(Optional::isPresent)
                         .map(Optional::get)
                         .collect(Collectors.toList());
        }
    }

    /**
     * Checks if the given style file is a CitationStyle
     */
    public static boolean isCitationStyleFile(String styleFile) {
        return StandardFileType.CITATION_STYLE.getExtensions().stream().anyMatch(styleFile::endsWith);
    }

    public String getTitle() {
        return title;
    }

    public boolean isNumericStyle() {
        return isNumericStyle;
    }

    public String getSource() {
        return source;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        CitationStyle other = (CitationStyle) o;
        return Objects.equals(source, other.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public boolean isInternalStyle() {
        return true;
    }

    @Override
    public String getPath() {
        return getFilePath();
    }
}
