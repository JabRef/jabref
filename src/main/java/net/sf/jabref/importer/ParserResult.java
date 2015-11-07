/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer;

import java.io.File;
import net.sf.jabref.model.entry.BibtexEntryType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.MetaData;

public class ParserResult {
    public static final ParserResult INVALID_FORMAT = new ParserResult(null, null, null);
    public static final ParserResult FILE_LOCKED = new ParserResult(null, null, null);
    private final BibtexDatabase base;
    private MetaData metaData;
    private final HashMap<String, BibtexEntryType> entryTypes;

    private File file;
    private final ArrayList<String> warnings = new ArrayList<>();
    private final ArrayList<String> duplicateKeys = new ArrayList<>();

    private String errorMessage;
    // Which encoding was used?
    private String encoding;

    private boolean postponedAutosaveFound;
    private boolean invalid;
    private boolean toOpenTab;

    // Which JabRef version wrote the file, if any?
    private String jabrefVersion;
    private int jabrefMajorVersion;
    private int jabrefMinorVersion;

    public ParserResult(Collection<BibtexEntry> entries) {
        this(ImportFormatReader.createDatabase(entries), null, new HashMap<String, BibtexEntryType>());
    }

    public ParserResult(BibtexDatabase base, MetaData metaData, HashMap<String, BibtexEntryType> entryTypes) {
        this.base = base;
        this.metaData = metaData;
        this.entryTypes = entryTypes;
    }

    /**
     * Check if this base is marked to be added to the currently open tab. Default is false.
     * @return
     */
    public boolean toOpenTab() {
        return toOpenTab;
    }

    public void setToOpenTab(boolean toOpenTab) {
        this.toOpenTab = toOpenTab;
    }

    /**
     * Find which version of JabRef, if any, produced this bib file.
     * @return The version number string, or null if no JabRef signature could be read.
     */
    public String getJabrefVersion() {
        return jabrefVersion;
    }

    /**
     * Set the JabRef version number string for this parser result.
     * @param jabrefVersion The version number string.
     */
    public void setJabrefVersion(String jabrefVersion) {
        this.jabrefVersion = jabrefVersion;
    }

    /**
     * @return 0 if not known (e.g., no version header in file)
     */
    public int getJabrefMajorVersion() {
        return jabrefMajorVersion;
    }

    public void setJabrefMajorVersion(int jabrefMajorVersion) {
        this.jabrefMajorVersion = jabrefMajorVersion;
    }

    /**
     * @return 0 if not known (e.g., no version header in file)
     */
    public int getJabrefMinorVersion() {
        return jabrefMinorVersion;
    }

    public void setJabrefMinorVersion(int jabrefMinorVersion) {
        this.jabrefMinorVersion = jabrefMinorVersion;
    }

    public BibtexDatabase getDatabase() {
        return base;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData md) {
        this.metaData = md;
    }

    public HashMap<String, BibtexEntryType> getEntryTypes() {
        return entryTypes;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File f) {
        file = f;
    }

    /**
     * Sets the variable indicating which encoding was used during parsing.
     *
     * @param enc String the name of the encoding.
     */
    public void setEncoding(String enc) {
        encoding = enc;
    }

    /**
     * Returns the name of the encoding used during parsing, or null if not specified
     * (indicates that prefs.get(JabRefPreferences.DEFAULT_ENCODING) was used).
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Add a parser warning.
     *
     * @param s String Warning text. Must be pretranslated. Only added if there isn't already a dupe.
     */
    public void addWarning(String s) {
        if (!warnings.contains(s)) {
            warnings.add(s);
        }
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public String[] warnings() {
        String[] s = new String[warnings.size()];
        for (int i = 0; i < warnings.size(); i++) {
            s[i] = warnings.get(i);
        }
        return s;
    }

    /**
     * Add a key to the list of duplicated BibTeX keys found in the database.
     * @param key The duplicated key
     */
    public void addDuplicateKey(String key) {
        if (!duplicateKeys.contains(key)) {
            duplicateKeys.add(key);
        }
    }

    /**
     * Query whether any duplicated BibTeX keys have been found in the database.
     * @return true if there is at least one duplicate key.
     */
    public boolean hasDuplicateKeys() {
        return !duplicateKeys.isEmpty();
    }

    /**
     * Get all duplicated keys found in the database.
     * @return An array containing the duplicated keys.
     */
    public String[] getDuplicateKeys() {
        return duplicateKeys.toArray(new String[duplicateKeys.size()]);
    }

    public boolean isPostponedAutosaveFound() {
        return postponedAutosaveFound;
    }

    public void setPostponedAutosaveFound(boolean postponedAutosaveFound) {
        this.postponedAutosaveFound = postponedAutosaveFound;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
