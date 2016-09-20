package net.sf.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.jabref.logic.util.FileExtensions;

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
 * Representation of a CitationStyle
 * Stores its name, the filepath and the style itself
 */
public class CitationStyle {

    public static final String DEFAULT = "/ieee.csl";
    private static final Log LOGGER = LogFactory.getLog(CitationStyle.class);

    private final String filepath;
    private final String title;
    private final String source;


    private CitationStyle(final String filename, final String title, final String source) {
        this.filepath = Objects.requireNonNull(filename);
        this.title = Objects.requireNonNull(title);
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Creates an CitationStyle instance out of the style string
     */
    private static CitationStyle createCitationStyleFromSource(final String source, final String filename) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(source));

            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("info");

            NodeList titleNode = ((Element) nodes.item(0)).getElementsByTagName("title");
            String title = ((CharacterData) titleNode.item(0).getFirstChild()).getData();

            return new CitationStyle(filename, title, source);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.error("Error while parsing source", e);
        }
        return null;
    }

    /**
     * Loads the CitationStyle from the given file
     */
    public static CitationStyle createCitationStyleFromFile(final String styleFile) {
        if (!isCitationStyleFile(styleFile)) {
            LOGGER.error("Can only load style files: "+ styleFile);
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
        } catch (NoSuchFileException e){
            LOGGER.error("Could not find file: "+ styleFile, e);
        } catch (IOException e) {
            LOGGER.error("Error reading source file", e);
        }
        return null;
    }

    public static CitationStyle getDefault(){
        return createCitationStyleFromFile(DEFAULT);
    }

    /**
     * THIS ONLY WORKS WHEN JabRef IS STARTED AS AN APPLICATION (JAR)
     *
     * Reads all available CitationStyle in the Jar
     */
    public static List<CitationStyle> discoverCitationStyles() {
        try {
            final List<CitationStyle> citationStyles = new ArrayList<>();
            String path = CitationStyle.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

            try (JarFile file = new JarFile(path)) {
                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    String filename = entries.nextElement().getName();
                    if (!filename.startsWith("dependent") && filename.endsWith("csl")) {
                        CitationStyle citationStyle = CitationStyle.createCitationStyleFromFile(filename);
                        if (citationStyle != null) {
                            citationStyles.add(citationStyle);
                        }
                    }
                }
            }
            return citationStyles;
        } catch (IOException | URISyntaxException ex) {
            LOGGER.error("something went wrong while searching available CitationStyles. " +
                    "Are you running directly from source code?", ex);
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the given style file is a CitationStyle
     */
    public static boolean isCitationStyleFile(String styleFile){
        return Arrays.stream(FileExtensions.CITATION_STYLE.getExtensions()).anyMatch(styleFile::endsWith);
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        CitationStyle that = (CitationStyle) other;
        return source != null ? source.equals(that.source) : that.source == null;
    }

}
