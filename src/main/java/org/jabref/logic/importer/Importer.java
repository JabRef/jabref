package org.jabref.logic.importer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;

import org.jabref.logic.util.FileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseModeDetection;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Role of an importer for JabRef.
 */
public abstract class Importer implements Comparable<Importer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

    /**
     * Check whether the source is in the correct format for this importer.
     * <p>
     * The effect of this method is primarily to avoid unnecessary processing of files when searching for a suitable
     * import format. If this method returns false, the import routine will move on to the next import format.
     * <p>
     * Thus the correct behaviour is to return false if it is certain that the file is not of the suitable type, and
     * true otherwise. Returning true is the safe choice if not certain.
     */
    public abstract boolean isRecognizedFormat(BufferedReader input) throws IOException;

    /**
     * Check whether the source is in the correct format for this importer.
     *
     * @param filePath the path of the file to check
     * @return true, if the file is in a recognized format
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public boolean isRecognizedFormat(Path filePath) throws IOException {
        try (BufferedReader bufferedReader = getReader(filePath)) {
            return isRecognizedFormat(bufferedReader);
        }
    }

    /**
     * Check whether the source is in the correct format for this importer.
     *
     * @param data the data to check
     * @return true, if the data is in a recognized format
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public boolean isRecognizedFormat(String data) throws IOException {
        try (StringReader stringReader = new StringReader(data);
             BufferedReader bufferedReader = new BufferedReader(stringReader)) {
            return isRecognizedFormat(bufferedReader);
        }
    }

    /**
     * Parse the database in the source.
     * <p>
     * This method can be called in two different contexts - either when importing in a specified format, or when
     * importing in unknown format. In the latter case, JabRef cycles through all available import formats. No error
     * messages or feedback is displayed from individual import formats in this case.
     * <p>
     * If importing in a specified format and an empty library is returned, JabRef reports that no entries were found.
     * <p>
     * This method should never return null.
     *
     * @param input the input to read from
     */
    public abstract ParserResult importDatabase(BufferedReader input) throws IOException;

    /**
     * Parse the database in the specified file.
     *
     * @param filePath the path to the file which should be imported
     */
    public ParserResult importDatabase(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath, StandardOpenOption.READ)) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            Charset charset = StandardCharsets.UTF_8;

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream, charset));
            ParserResult parserResult = importDatabase(bufferedReader);

            // store the detected encoding
            parserResult.getMetaData().setEncoding(charset);

            parserResult.setPath(filePath);

            // Make sure the mode is always set
            if (parserResult.getMetaData().getMode().isEmpty()) {
                parserResult.getMetaData().setMode(BibDatabaseModeDetection.inferMode(parserResult.getDatabase()));
            }
            return parserResult;
        }
    }

    protected static Charset getCharset(BufferedInputStream bufferedInputStream) {
        Charset defaultCharSet = StandardCharsets.UTF_8;

        // This reads the first 8000 bytes only, thus the default size of 8192 of the bufferedInputStream is OK.
        // See https://github.com/unicode-org/icu/blob/06ef8867f35befee7340e35082fefc9d3561d230/icu4j/main/classes/core/src/com/ibm/icu/text/CharsetDetector.java#L125 for details
        CharsetDetector charsetDetector = new CharsetDetector();
        try {
            charsetDetector.setText(bufferedInputStream);

            CharsetMatch[] matches = charsetDetector.detectAll();
            if ((matches == null) || (matches.length == 0)) {
                return defaultCharSet;
            }
            if (Arrays.stream(matches).anyMatch(singleCharset -> singleCharset.getName().equals(defaultCharSet.toString()))) {
                return defaultCharSet;
            }
            if (Arrays.stream(matches).anyMatch(singleCharset -> singleCharset.getName().equals(StandardCharsets.UTF_16.toString()))) {
                return StandardCharsets.UTF_16;
            }

            if (matches[0] != null) {
                return Charset.forName(matches[0].getName());
            }
        } catch (IOException e) {
            LOGGER.error("Could not determine charset. Using default one.", e);
        }
        return defaultCharSet;
    }

    /**
     * Parse the database in the specified string.
     * <p>
     * Importer having the facilities to detect the correct encoding of a string should overwrite this method, determine
     * the encoding and then call {@link #importDatabase(BufferedReader)}.
     *
     * @param data the string which should be imported
     * @return the parsed result
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public ParserResult importDatabase(String data) throws IOException {
        try (StringReader stringReader = new StringReader(data);
             BufferedReader bufferedReader = new BufferedReader(stringReader)) {
            return importDatabase(bufferedReader);
        }
    }

    public static BufferedReader getReader(Path filePath) throws IOException {
        InputStream stream = Files.newInputStream(filePath, StandardOpenOption.READ);

        if (FileUtil.isBibFile(filePath)) {
            return getReader(stream);
        }

        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    public static BufferedReader getReader(InputStream stream) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
        Charset charset = getCharset(bufferedInputStream);
        InputStreamReader reader = new InputStreamReader(bufferedInputStream, charset);
        return new BufferedReader(reader);
    }

    /**
     * Returns the name of this import format.
     *
     * <p>The name must be unique.</p>
     *
     * @return format name, must be unique and not <code>null</code>
     */
    public abstract String getName();

    /**
     * Returns the type of files that this importer can read
     *
     * @return {@link FileType} corresponding to the importer
     */
    public abstract FileType getFileType();

    /**
     * Returns a one-word ID which identifies this importer. Used for example, to identify the importer when used from
     * the command line.
     *
     * @return ID, must be unique and not <code>null</code>
     */
    public String getId() {
        String id = getName();
        StringBuilder result = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /**
     * Returns the description of the import format.
     * <p>
     * The description should specify
     * <ul><li>
     *   what kind of entries from what sources and based on what specification it is able to import
     * </li><li>
     *   by what criteria it {@link #isRecognizedFormat(BufferedReader) recognizes} an import format
     * </li></ul>
     *
     * @return description of the import format
     */
    public abstract String getDescription();

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Importer)) {
            return false;
        }
        Importer other = (Importer) obj;
        return Objects.equals(this.getName(), other.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(Importer o) {
        return getName().compareTo(o.getName());
    }
}
