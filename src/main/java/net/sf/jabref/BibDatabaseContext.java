package net.sf.jabref;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.BibDatabaseModeDetection;

/**
 * Represents everything related to a .bib file.
 * <p>
 * The entries are stored in BibDatabase, the other data in MetaData and the options relevant for this file in Defaults.
 */
public class BibDatabaseContext {

    private final BibDatabase database;
    private final MetaData metaData;
    private final Defaults defaults;
    /** The file where this database was last saved to. */
    private File file;

    public BibDatabaseContext() {
        this(new Defaults());
    }

    public BibDatabaseContext(Defaults defaults) {
        this(new BibDatabase(), defaults);
    }

    public BibDatabaseContext(BibDatabase database) {
        this(database, new Defaults());
    }

    public BibDatabaseContext(BibDatabase database, Defaults defaults) {
        this(database, new MetaData(), defaults);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, Defaults defaults) {
        this.defaults = Objects.requireNonNull(defaults);
        this.database = Objects.requireNonNull(database);
        this.metaData = Objects.requireNonNull(metaData);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData) {
        this(database, metaData, new Defaults());
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, File file, Defaults defaults) {
        this(database, metaData, defaults);

        this.setDatabaseFile(file);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, File file) {
        this(database, metaData, file, new Defaults());
    }

    public BibDatabaseMode getMode() {
        Optional<BibDatabaseMode> mode = metaData.getMode();

        if (!mode.isPresent()) {
            BibDatabaseMode inferredMode = BibDatabaseModeDetection.inferMode(database);
            BibDatabaseMode newMode = BibDatabaseMode.BIBTEX;
            if ((defaults.mode == BibDatabaseMode.BIBLATEX) || (inferredMode == BibDatabaseMode.BIBLATEX)) {
                newMode =  BibDatabaseMode.BIBLATEX;
            }
            this.setMode(newMode);
            return newMode;
        }
        return mode.get();
    }

    public void setMode(BibDatabaseMode bibDatabaseMode) {
        metaData.setMode(bibDatabaseMode);
    }

    /**
     * Get the file where this database was last saved to or loaded from, if any.
     *
     * @return The relevant File, or null if none is defined.
     */
    public File getDatabaseFile() {
        return file;
    }

    public void setDatabaseFile(File file) {
        this.file = file;
    }

    public BibDatabase getDatabase() {
        return database;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public boolean isBiblatexMode() {
        return getMode() == BibDatabaseMode.BIBLATEX;
    }

    /**
     * Look up the directory set up for the given field type for this database.
     * If no directory is set up, return that defined in global preferences.
     * There can be up to three directory definitions for these files:
     * the database's metadata can specify a general directory and/or a user-specific directory
     * or the preferences can specify one.
     * <p>
     * The settings are prioritized in the following order and the first defined setting is used:
     * 1. metadata user-specific directory
     * 2. metadata general directory
     * 3. preferences directory
     * 4. bib file directory
     *
     * @param fieldName The field type
     * @return The default directory for this field type.
     */
    public List<String> getFileDirectory(String fieldName) {
        List<String> fileDirs = new ArrayList<>();

        // 1. metadata user-specific directory
        Optional<String> userFileDirectory = metaData.getUserFileDirectory(Globals.prefs.getUser());
        if(userFileDirectory.isPresent()) {
            fileDirs.add(getFileDirectoryPath(userFileDirectory.get()));
        }

        // 2. metadata general directory
        Optional<String> metaDataDirectory = metaData.getDefaultFileDirectory();
        if(metaDataDirectory.isPresent()) {
            fileDirs.add(getFileDirectoryPath(metaDataDirectory.get()));
        }

        // 3. preferences directory
        String dir = Globals.prefs.get(fieldName + Globals.DIR_SUFFIX); // FILE_DIR
        if (dir != null) {
            fileDirs.add(dir);
        }

        // 4. bib file directory
        if (getDatabaseFile() != null) {
            String parentDir = getDatabaseFile().getParent();
            // Check if we should add it as primary file dir (first in the list) or not:
            if (Globals.prefs.getBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR)) {
                fileDirs.add(0, parentDir);
            } else {
                fileDirs.add(parentDir);
            }
        }

        return fileDirs;
    }

    private String getFileDirectoryPath(String directoryName) {
        String dir = directoryName;
        // If this directory is relative, we try to interpret it as relative to
        // the file path of this bib file:
        if (!new File(dir).isAbsolute() && (getDatabaseFile() != null)) {
            String relDir;
            if (".".equals(dir)) {
                // if dir is only "current" directory, just use its parent (== real current directory) as path
                relDir = getDatabaseFile().getParent();
            } else {
                relDir = getDatabaseFile().getParent() + File.separator + dir;
            }
            // If this directory actually exists, it is very likely that the
            // user wants us to use it:
            if (new File(relDir).exists()) {
                dir = relDir;
            }
        }
        return dir;
    }

    public List<String> getFileDirectory() {
        return getFileDirectory(Globals.FILE_FIELD);
    }
}
