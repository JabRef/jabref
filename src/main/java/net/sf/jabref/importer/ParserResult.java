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
import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.MetaData;
import net.sf.jabref.model.entry.EntryType;

public class ParserResult {
    public static final ParserResult INVALID_FORMAT = new ParserResult(null, null, null);
    public static final ParserResult FILE_LOCKED = new ParserResult(null, null, null);
    private final BibtexDatabase base;
    private MetaData metaData;
    private final HashMap<String, EntryType> entryTypes;

    private File file;
    private final ArrayList<String> warnings = new ArrayList<>();
    private final ArrayList<String> duplicateKeys = new ArrayList<>();

    private String errorMessage;
    // Which encoding was used?
    private Charset encoding;

    private boolean postponedAutosaveFound;
    private boolean invalid;
    private boolean toOpenTab;

    public ParserResult(Collection<BibtexEntry> entries) {
        this(ImportFormatReader.createDatabase(entries), null, new HashMap<>());
    }

    public ParserResult(BibtexDatabase base, MetaData metaData, HashMap<String, EntryType> entryTypes) {
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

    public BibtexDatabase getDatabase() {
        return base;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData md) {
        this.metaData = md;
    }

    public HashMap<String, EntryType> getEntryTypes() {
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
     * @param enc the encoding.
     */
    public void setEncoding(Charset enc) {
        encoding = enc;
    }

    /**
     * Returns the encoding used during parsing, or null if not specified (indicates that
     * prefs.get(JabRefPreferences.DEFAULT_ENCODING) was used).
     */
    public Charset getEncoding() {
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
