package net.sf.jabref.imports;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

/**
 * This interface defines the role of an importer for JabRef.
 */
public interface ImportFormat {

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream in) throws IOException;

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List importEntries(InputStream in) throws IOException;


    /**
     * Return the name of this import format.
     */
    public String getFormatName();
}
