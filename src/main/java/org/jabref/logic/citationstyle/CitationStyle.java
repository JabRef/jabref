package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.util.FileExtensions;

import de.undercouch.citeproc.helper.CSLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Representation of a CitationStyle.
 * Stores its name, the file path and the style itself
 */
public class CitationStyle {

    public static final String DEFAULT = "/ieee.csl";
    private static final Log LOGGER = LogFactory.getLog(CitationStyle.class);

    private static final Pattern SNAPSHOT_NAME = Pattern.compile(".*styles-1\\.0\\.1-SNAPSHOT\\.jar");

    private static final List<CitationStyle> STYLES = new ArrayList<>();

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
    private static CitationStyle createCitationStyleFromSource(final String source, final String filename) {
        if ((filename != null) && !filename.isEmpty() && (source != null) && !source.isEmpty()) {
            try {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(stripInvalidProlog(source)));

                Document doc = db.parse(is);
                NodeList nodes = doc.getElementsByTagName("info");

                NodeList titleNode = ((Element) nodes.item(0)).getElementsByTagName("title");
                String title = ((CharacterData) titleNode.item(0).getFirstChild()).getData();

                return new CitationStyle(filename, title, source);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                LOGGER.error("Error while parsing source", e);
            }
        }
        return null;
    }

    private static String stripInvalidProlog(String source) {
        int startIndex = source.indexOf("<");
        if (startIndex > 0) {
            return source.substring(startIndex, source.length());
        } else {
            return source;
        }
    }

    /**
     * Loads the CitationStyle from the given file
     */
    public static CitationStyle createCitationStyleFromFile(final String styleFile) {
        if (!isCitationStyleFile(styleFile)) {
            LOGGER.error("Can only load style files: " + styleFile);
            return null;
        }

        try {
            String text;
            String internalFile = (styleFile.startsWith("/") ? "" : "/") + styleFile;
            URL url = CitationStyle.class.getResource(internalFile);
            if (url != null) {
                text = CSLUtils.readURLToString(url, StandardCharsets.UTF_8.toString());
            } else {
                // if the url is null then the style is located outside the classpath
                text = new String(Files.readAllBytes(Paths.get(styleFile)), StandardCharsets.UTF_8);
            }
            return createCitationStyleFromSource(text, styleFile);
        } catch (NoSuchFileException e) {
            LOGGER.error("Could not find file: " + styleFile, e);
        } catch (IOException e) {
            LOGGER.error("Error reading source file", e);
        }
        return null;
    }

    /**
     * Provides the default citation style which is currently IEEE
     * @return default citation style
     */
    public static CitationStyle getDefault() {
        return createCitationStyleFromFile(DEFAULT);
    }

    /**
     * Provides the citation styles that come with JabRef.
     * @return list of available citation styles
     */
    public static List<CitationStyle> discoverCitationStyles() {
        if (!STYLES.isEmpty()) {
            return STYLES;
        }
        try {

            Path filePath = Paths.get(CitationStyle.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String path = filePath.toString();

            // This is a quick fix to have the styles when running JabRef in a development environment.
            // The styles.jar is not extracted into the JabRef.jar and therefore, we search the classpath for it.
            if (Files.isDirectory(filePath)) {
                final String cp = System.getProperty("java.class.path");
                final String[] entries = cp.split(System.getProperty("path.separator"));

                Optional<String> foundStyle = Arrays.stream(entries).filter(entry -> SNAPSHOT_NAME.matcher(entry).matches()).findFirst();
                path = foundStyle.orElse(path);
            }

            try (FileSystem jarFs = FileSystems.newFileSystem(Paths.get(path), null)) {

                List<Path> allStyles = Files.find(jarFs.getRootDirectories().iterator().next(), 1, (file, attr) -> file.toString().endsWith("csl")).collect(Collectors.toList());

                for (Path style : allStyles) {
                    CitationStyle citationStyle = CitationStyle.createCitationStyleFromFile(style.getFileName().toString());
                    if (citationStyle != null) {
                        STYLES.add(citationStyle);
                    }
                }
            }
            return STYLES;
        } catch (IOException | URISyntaxException ex) {
            LOGGER.error("something went wrong while searching available CitationStyles. Are you running directly from source code?", ex);
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the given style file is a CitationStyle
     */
    public static boolean isCitationStyleFile(String styleFile) {
        return Arrays.stream(FileExtensions.CITATION_STYLE.getExtensions()).anyMatch(styleFile::endsWith);
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

}
