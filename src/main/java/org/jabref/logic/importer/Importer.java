package org.jabref.logic.importer;

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
import java.util.Objects;

import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseModeDetection;

/**
 * Role of an importer for JabRef.
 */
public abstract class Importer implements Comparable<Importer> {

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
     * @param encoding the encoding of the file
     * @return true, if the file is in a recognized format
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public boolean isRecognizedFormat(Path filePath, Charset encoding) throws IOException {
        try (BufferedReader bufferedReader = getReader(filePath, encoding)) {
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
     * <p>
     * Importer having the facilities to detect the correct encoding of a file should overwrite this method, determine
     * the encoding and then call {@link #importDatabase(BufferedReader)}.
     *
     * @param filePath the path to the file which should be imported
     * @param encoding the encoding used to decode the file
     */
    public ParserResult importDatabase(Path filePath, Charset encoding) throws IOException {
        try (BufferedReader bufferedReader = getReader(filePath, encoding)) {
            ParserResult parserResult = importDatabase(bufferedReader);
            parserResult.getMetaData().setEncoding(encoding);
            parserResult.setPath(filePath);

            // Make sure the mode is always set
            if (parserResult.getMetaData().getMode().isEmpty()) {
                parserResult.getMetaData().setMode(BibDatabaseModeDetection.inferMode(parserResult.getDatabase()));
            }
            return parserResult;
        }
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

    protected static BufferedReader getUTF8Reader(Path filePath) throws IOException {
        return getReader(filePath, StandardCharsets.UTF_8);
    }

    protected static BufferedReader getUTF16Reader(Path filePath) throws IOException {
        return getReader(filePath, StandardCharsets.UTF_16);
    }

    public static BufferedReader getReader(Path filePath, Charset encoding)
            throws IOException {
        InputStream stream = Files.newInputStream(filePath, StandardOpenOption.READ);
        return new BufferedReader(new InputStreamReader(stream, encoding));
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
