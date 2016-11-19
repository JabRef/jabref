package net.sf.jabref.logic.importer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import net.sf.jabref.logic.util.FileExtensions;

/**
 * Role of an importer for JabRef.
 */
public abstract class Importer implements Comparable<Importer> {

    /**
     * Using this when I have no database open or when I read
     * non bibtex file formats (used by the ImportFormatReader.java)
     *
     * TODO: Is this field really needed or would calling IdGenerator.next() suffice?
     */
    public static final String DEFAULT_BIBTEXENTRY_ID = "__ID";

    /**
     * Check whether the source is in the correct format for this importer.
     *
     * The effect of this method is primarily to avoid unnecessary processing of
     * files when searching for a suitable import format. If this method returns
     * false, the import routine will move on to the next import format.
     *
     * Thus the correct behaviour is to return false if it is certain that the file is
     * not of the suitable type, and true otherwise. Returning true is the safe choice if not certain.
     */
    public abstract boolean isRecognizedFormat(BufferedReader input) throws IOException;

    public boolean isRecognizedFormat(Path filePath, Charset encoding) throws IOException {
        try (BufferedReader bufferedReader = getReader(filePath, encoding)) {
            return isRecognizedFormat(bufferedReader);
        }
    }

    /**
     * Parse the database in the source.
     *
     * This method can be called in two different contexts - either when importing in
     * a specified format, or when importing in unknown format. In the latter case,
     * JabRef cycles through all available import formats. No error messages or feedback
     * is displayed from individual import formats in this case.
     *
     * If importing in a specified format and an empty database is returned, JabRef reports
     * that no entries were found.
     *
     * This method should never return null.
     *
     * @param input the input to read from
     */
    public abstract ParserResult importDatabase(BufferedReader input) throws IOException ;

    /**
     * Parse the database in the specified file.
     *
     * Importer having the facilities to detect the correct encoding of a file should overwrite this method,
     * determine the encoding and then call {@link #importDatabase(BufferedReader)}.
     *
     * @param filePath the path to the file which should be imported
     * @param encoding the encoding used to decode the file
     */
    public ParserResult importDatabase(Path filePath, Charset encoding) throws IOException {
        try (BufferedReader bufferedReader = getReader(filePath, encoding)) {
            ParserResult parserResult = importDatabase(bufferedReader);
            parserResult.getMetaData().setEncoding(encoding);
            parserResult.setFile(filePath.toFile());
            return parserResult;
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
        InputStream stream = new FileInputStream(filePath.toFile());
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
     * Returns the file extensions that this importer can read
     * @return {@link FileExtensions} correspoding to the importer
     */
    public abstract FileExtensions getExtensions();

    /**
     * Returns a one-word ID which identifies this import format.
     * Used for example, to identify the format when used from the command line.
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
     *
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
        if(!(obj instanceof Importer)) {
            return false;
        }
        Importer other = (Importer)obj;
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
