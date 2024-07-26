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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.util.StandardFileType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Representation of a CitationStyle. Stores its name, the file path and the style itself
 */
@AllowedToUseClassGetResource("org.jabref.logic.citationstyle.CitationStyle.discoverCitationStyles reads the whole path to discover all available styles. Should be converted to a build-time job.")
public class CitationStyle implements OOStyle {

    public static final String DEFAULT = "/ieee.csl";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyle.class);
    private static final String STYLES_ROOT = "/csl-styles";
    private static final List<CitationStyle> STYLES = new ArrayList<>();
    private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();

    private final String filePath;
    private final String title;
    private final String source;

    private CitationStyle(final String filename, final String title, final String source) {
        this.filePath = Objects.requireNonNull(filename);
        this.title = Objects.requireNonNull(title);
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Creates an CitationStyle instance out of the style string
     */
    private static Optional<CitationStyle> createCitationStyleFromSource(final InputStream source, final String filename) {
        try {
            // We need the content twice:
            //   First, for parsing it here for the name
            //   Second for the CSL library to parse it
            String content = new String(source.readAllBytes());

            Optional<String> title = getTitle(filename, content);
            if (title.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new CitationStyle(filename, title.get(), content));
        } catch (ParserConfigurationException | SAXException | NullPointerException | IOException e) {
            LOGGER.error("Error while parsing source", e);
            return Optional.empty();
        }
    }

    private static Optional<String> getTitle(String filename, String content) throws SAXException, IOException, ParserConfigurationException {
        // TODO: Switch to StAX parsing (to speed up - we need only the title)
        InputSource inputSource = new InputSource(new StringReader(content));
        Document doc = FACTORY.newDocumentBuilder().parse(inputSource);

        // See CSL#canFormatBibliographies, checks if the tag exists
        NodeList bibs = doc.getElementsByTagName("bibliography");
        if (bibs.getLength() <= 0) {
            LOGGER.debug("no bibliography element for file {} ", filename);
            return Optional.empty();
        }

        NodeList nodes = doc.getElementsByTagName("info");
        NodeList titleNode = ((Element) nodes.item(0)).getElementsByTagName("title");
        String title = ((CharacterData) titleNode.item(0).getFirstChild()).getData();
        return Optional.of(title);
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
        return createCitationStyleFromFile(DEFAULT).orElse(new CitationStyle("", "Empty", ""));
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
        } catch (URISyntaxException | IOException e) {
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
