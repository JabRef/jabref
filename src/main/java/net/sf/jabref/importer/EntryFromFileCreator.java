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
import java.util.List;
import java.util.StringTokenizer;

import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;

/**
 * The interface EntryFromFileCreator does twice: <br>
 * On the one hand, it defines a set of files, which it can deal with, on the
 * other hand it provides the functionality to create a Bibtex entry out of a
 * file. The interface extends the java.io.FileFilter to inherit a common way of
 * defining file sets.
 *
 * @author Dan&Nosh
 * @version 25.11.2008 | 23:39:03
 *
 */
public abstract class EntryFromFileCreator implements java.io.FileFilter {

    final ExternalFileType externalFileType;


    /**
     * Constructor. <br>
     * Forces subclasses to provide an {@link ExternalFileType} instance, which
     * they build on.
     *
     * @param externalFileType
     */
    EntryFromFileCreator(ExternalFileType externalFileType) {
        this.externalFileType = externalFileType;
    }

    protected abstract BibtexEntry createBibtexEntry(File f);

    /**
     * <p>
     * To support platform independence, a creator must define what types of
     * files it accepts on it's own.
     * </p>
     * <p>
     * Basically, accepting files which end with the file extension that is
     * described in the nested {@link #externalFileType} would work on windows
     * systems. This is also the recommended criterion, on which files should be
     * accepted.
     * </p>
     * <p>
     * However, defining what types of files this creator accepts, is a property
     * of <i>entry creators</i>, that is left to the user.
     * </p>
     */
    @Override
    public abstract boolean accept(File f);

    /**
     * Name of this import format.
     *
     * <p>
     * The name must be unique.
     * </p>
     *
     * @return format name, must be unique and not <code>null</code>
     */
    public abstract String getFormatName();

    /**
     * Create one BibtexEntry containing information regarding the given File.
     *
     * @param f
     * @param addPathTokensAsKeywords
     * @return
     */
    public BibtexEntry createEntry(File f, boolean addPathTokensAsKeywords) {
        if ((f == null) || !f.exists()) {
            return null;
        }
        BibtexEntry newEntry = createBibtexEntry(f);

        if (newEntry == null) {
            return null;
        }

        if (addPathTokensAsKeywords) {
            appendToField(newEntry, "keywords", extractPathesToKeyWordsfield(f.getAbsolutePath()));
        }

        if (newEntry.getField("title") == null) {
            newEntry.setField("title", f.getName());
        }

        addFileInfo(newEntry, f);
        return newEntry;
    }

    /** Returns the ExternalFileType that is imported here */
    public ExternalFileType getExternalFileType() {
        return externalFileType;
    }

    /**
     * Splits the path to the file and builds a keywords String in the format
     * that is used by Jabref.
     *
     * @param absolutePath
     * @return
     */
    private static String extractPathesToKeyWordsfield(String absolutePath) {
        final int MIN_PATH_TOKEN_LENGTH = 4;
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(absolutePath, "" + File.separatorChar);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (!st.hasMoreTokens()) {
                // ignore last token. The filename ist not wanted as keyword.
                break;
            }
            if (token.length() >= MIN_PATH_TOKEN_LENGTH) {
                if (sb.length() > 0) {
                    // TODO: find Jabref constant for delimter
                    sb.append(",");
                }
                sb.append(token);
            }
        }
        return sb.toString();
    }

    private void addFileInfo(BibtexEntry entry, File file) {
        JabRefPreferences jabRefPreferences = JabRefPreferences.getInstance();
        ExternalFileType fileType = jabRefPreferences.getExternalFileTypeByExt(externalFileType.getFieldName());

        String[] possibleFilePaths = JabRef.jrf.basePanel().metaData().getFileDirectory(Globals.FILE_FIELD);
        File shortenedFileName = FileUtil.shortenFileName(file, possibleFilePaths);
        FileListEntry fileListEntry = new FileListEntry("", shortenedFileName.getPath(), fileType);

        FileListTableModel model = new FileListTableModel();
        model.addEntry(0, fileListEntry);

        entry.setField("file", model.getStringRepresentation());
    }

    void appendToField(BibtexEntry entry, String field, String value) {
        if ((value == null) || "".equals(value)) {
            return;
        }
        String oVal = entry.getField(field);
        if (oVal == null) {
            entry.setField(field, value);
        } else {
            // TODO: find Jabref constant for delimter
            if (!oVal.contains(value)) {
                entry.setField(field, oVal + "," + value);
            }

        }
    }

    void addEntrysToEntry(BibtexEntry entry, List<BibtexEntry> entrys) {
        if (entrys != null) {
            for (BibtexEntry e : entrys) {
                addEntryDataToEntry(entry, e);
            }
        }
    }

    void addEntryDataToEntry(BibtexEntry entry, BibtexEntry e) {
        for (String field : e.getAllFields()) {
            appendToField(entry, field, e.getField(field));
        }
    }

    @Override
    public String toString() {
        if (externalFileType == null) {
            return "(undefined)";
        }
        return externalFileType.getName() + " (." + externalFileType.getExtension() + ")";
    }

}
