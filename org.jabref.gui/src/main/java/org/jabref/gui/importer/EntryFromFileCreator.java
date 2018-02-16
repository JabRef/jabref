package org.jabref.gui.importer;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

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
public abstract class EntryFromFileCreator implements FileFilter {

    private static final int MIN_PATH_TOKEN_LENGTH = 4;

    protected final ExternalFileType externalFileType;

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

    /**
     * Splits the path to the file and builds a keywords String in the format
     * that is used by Jabref.
     */
    private static String extractPathesToKeyWordsfield(String absolutePath) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(absolutePath, String.valueOf(File.separatorChar));
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (!st.hasMoreTokens()) {
                // ignore last token. The filename ist not wanted as keyword.
                break;
            }
            if (token.length() >= MIN_PATH_TOKEN_LENGTH) {
                if (sb.length() > 0) {
                    // TODO: find Jabref constant for delimter
                    sb.append(',');
                }
                sb.append(token);
            }
        }
        return sb.toString();
    }

    protected abstract Optional<BibEntry> createBibtexEntry(File f);

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
     * Create one BibEntry containing information regarding the given File.
     *
     * @param f
     * @param addPathTokensAsKeywords
     * @return
     */
    public Optional<BibEntry> createEntry(File f, boolean addPathTokensAsKeywords) {
        if ((f == null) || !f.exists()) {
            return Optional.empty();
        }
        Optional<BibEntry> newEntry = createBibtexEntry(f);

        if (!(newEntry.isPresent())) {
            return newEntry;
        }

        if (addPathTokensAsKeywords) {
            appendToField(newEntry.get(), FieldName.KEYWORDS, extractPathesToKeyWordsfield(f.getAbsolutePath()));
        }

        if (!newEntry.get().hasField(FieldName.TITLE)) {
            newEntry.get().setField(FieldName.TITLE, f.getName());
        }

        addFileInfo(newEntry.get(), f);
        return newEntry;
    }

    /** Returns the ExternalFileType that is imported here */
    public ExternalFileType getExternalFileType() {
        return externalFileType;
    }

    private void addFileInfo(BibEntry entry, File file) {
        Optional<ExternalFileType> fileType = ExternalFileTypes.getInstance()
                .getExternalFileTypeByExt(externalFileType.getFieldName());

        List<Path> possibleFilePaths = JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext()
                .getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
        Path shortenedFileName = FileUtil.shortenFileName(file.toPath(), possibleFilePaths);
        FileListEntry fileListEntry = new FileListEntry("", shortenedFileName.toString(), fileType);

        FileListTableModel model = new FileListTableModel();
        model.addEntry(0, fileListEntry);

        entry.setField(FieldName.FILE, model.getStringRepresentation());
    }

    protected void appendToField(BibEntry entry, String field, String value) {
        if ((value == null) || value.isEmpty()) {
            return;
        }
        Optional<String> oVal = entry.getField(field);
        if (oVal.isPresent()) {
            // TODO: find Jabref constant for delimter
            if (!oVal.get().contains(value)) {
                entry.setField(field, oVal.get() + "," + value);
            }
        } else {
            entry.setField(field, value);
        }
    }

    protected void addEntrysToEntry(BibEntry entry, List<BibEntry> entrys) {
        if (entrys != null) {
            for (BibEntry e : entrys) {
                addEntryDataToEntry(entry, e);
            }
        }
    }

    protected void addEntryDataToEntry(BibEntry entry, BibEntry e) {
        for (String field : e.getFieldNames()) {
            e.getField(field).ifPresent(fieldContent -> appendToField(entry, field, fieldContent));
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
