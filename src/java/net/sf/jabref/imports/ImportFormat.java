package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.sf.jabref.BibtexEntry;

/**
 * Role of an importer for JabRef.
 * 
 * <p>Importers are sorted according to following criteria
 * <ol><li>
 *   custom importers come first, then importers shipped with JabRef
 * </li><li>
 *   then importers are sorted by name.
 * </li></ol>
 * </p>
 */
public abstract class ImportFormat implements Comparable<ImportFormat> {

    private boolean isCustomImporter;
    
    /**
     * Constructor for custom importers.
     */
    public ImportFormat() {
      this.isCustomImporter = false;
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public abstract boolean isRecognizedFormat(InputStream in) throws IOException;

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public abstract List<BibtexEntry> importEntries(InputStream in) throws IOException;


    /**
     * Name of this import format.
     * 
     * <p>The name must be unique.</p>
     * 
     * @return format name, must be unique and not <code>null</code>
     */
    public abstract String getFormatName();
    
    /**
     * Extensions that this importer can read.
     * 
     * @return comma separated list of extensions or <code>null</code> for the default
     */
    public String getExtensions() {
      return null;
    }
    
    /**
     * Short, one token ID to identify the format from the command line.
     * 
     * @return command line ID
     */
    public String getCLIId() {
      String id = getFormatName();
      StringBuffer result = new StringBuffer(id.length());
      for (int i = 0; i < id.length(); i++) {
        char c = id.charAt(i);
        if (Character.isLetterOrDigit(c)) {
          result.append(Character.toLowerCase(c));
        }
      }
      return result.toString();
    }
    
    /**
     * Description  of the ImportFormat.
     * 
     * <p>Implementors of ImportFormats should override this. Ideally, it should specify
     * <ul><li>
     *   what kind of entries from what sources and based on what specification it is able to import
     * </li><li>
     *   by what criteria it {@link #isRecognizedFormat(InputStream) recognizes} an import format
     * </li></ul>
     * 
     * @return description of the import format
     */
    public String getDescription() {
      return "No description available for " + getFormatName() + ".";
    }
    
    /**
     * Sets if this is a custom importer.
     * 
     * <p>For custom importers added dynamically to JabRef, this will be
     * set automatically by JabRef.</p>
     * 
     * @param isCustomImporter if this is a custom importer
     */
    public final void setIsCustomImporter(boolean isCustomImporter) {
      this.isCustomImporter = isCustomImporter;
    }
    
    /**
     * Wether this importer is a custom importer.
     * 
     * <p>Custom importers will have precedence over built-in importers.</p>
     * 
     * @return  wether this is a custom importer
     */
    public final boolean getIsCustomImporter() {
      return this.isCustomImporter; 
    }
        
    /*
     *  (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return getFormatName().hashCode();
    }
    
    /*
     *  (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
      return o != null 
          && o instanceof ImportFormat
          && ((ImportFormat)o).getIsCustomImporter() == getIsCustomImporter() 
          && ((ImportFormat)o).getFormatName().equals(getFormatName());
    }
    
    /*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return getFormatName();
    }
    
    /*
     *  (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ImportFormat importer) {
      int result = 0;
      if (getIsCustomImporter() == importer.getIsCustomImporter()) {
        result = getFormatName().compareTo(importer.getFormatName());
      } else {
        result = getIsCustomImporter() ? 1 : -1;
      }
      return result;
    }
}
