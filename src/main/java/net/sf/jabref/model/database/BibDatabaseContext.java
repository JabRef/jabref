package net.sf.jabref.model.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.shared.DBMSSynchronizer;

/**
 * Represents everything related to a BIB file.
 * <p>
 * The entries are stored in BibDatabase, the other data in MetaData and the options relevant for this file in Defaults.
 */
public class BibDatabaseContext {

    private final BibDatabase database;
    private MetaData metaData;
    private final Defaults defaults;
    /** The file where this database was last saved to. */
    private File file;
    private DBMSSynchronizer dbmsSynchronizer;
    private DatabaseLocation location;

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
        this.location = DatabaseLocation.LOCAL;
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData) {
        this(database, metaData, new Defaults());
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, File file, Defaults defaults, DatabaseLocation location) {
        this(database, metaData, defaults);
        Objects.requireNonNull(location);
        this.setDatabaseFile(file);

        if (location == DatabaseLocation.LOCAL) {
            convertToLocalDatabase();
        }
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, File file, Defaults defaults) {
        this(database, metaData, file, defaults, DatabaseLocation.LOCAL);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, File file) {
        this(database, metaData, file, new Defaults());
    }

    public BibDatabaseContext(Defaults defaults, DatabaseLocation location, Character keywordSeparator,
            GlobalBibtexKeyPattern globalCiteKeyPattern) {
        this(new BibDatabase(), new MetaData(), defaults);
        if (location == DatabaseLocation.SHARED) {
            convertToSharedDatabase(keywordSeparator, globalCiteKeyPattern);
        }
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
     * @return Optional of the relevant File, or Optional.empty() if none is defined.
     */
    public Optional<File> getDatabaseFile() {
        return Optional.ofNullable(file);
    }

    public void setDatabaseFile(File file) {
        this.file = file;
    }

    public void clearDatabaseFile() {
        this.file = null;
    }

    public BibDatabase getDatabase() {
        return database;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = Objects.requireNonNull(metaData);
    }

    public boolean isBiblatexMode() {
        return getMode() == BibDatabaseMode.BIBLATEX;
    }

    public List<String> getFileDirectory(FileDirectoryPreferences preferences) {
        return getFileDirectory(FieldName.FILE, preferences);
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
    * 4. BIB file directory
    *
    * @param
    * @param fieldName The field type
    * @return The default directory for this field type.
    */
    public List<String> getFileDirectory(String fieldName, FileDirectoryPreferences preferences) {
        List<String> fileDirs = new ArrayList<>();

        // 1. metadata user-specific directory
        Optional<String> userFileDirectory = metaData.getUserFileDirectory(preferences.getUser());
        if(userFileDirectory.isPresent()) {
            fileDirs.add(getFileDirectoryPath(userFileDirectory.get()));
        }

        // 2. metadata general directory
        Optional<String> metaDataDirectory = metaData.getDefaultFileDirectory();
        if(metaDataDirectory.isPresent()) {
            fileDirs.add(getFileDirectoryPath(metaDataDirectory.get()));
        }

        // 3. preferences directory
        preferences.getFileDirectory(fieldName).ifPresent(fileDirs::add);

        // 4. BIB file directory
        getDatabaseFile().ifPresent(databaseFile -> {
            String parentDir = databaseFile.getParent();
            // Check if we should add it as primary file dir (first in the list) or not:
            if (preferences.isBibLocationAsPrimary()) {
                fileDirs.add(0, parentDir);
            } else {
                fileDirs.add(parentDir);
            }
        });

        return fileDirs;
    }

    private String getFileDirectoryPath(String directoryName) {
        String dir = directoryName;
        // If this directory is relative, we try to interpret it as relative to
        // the file path of this BIB file:
        Optional<File> databaseFile = getDatabaseFile();
        if (!new File(dir).isAbsolute() && databaseFile.isPresent()) {
            String relDir;
            if (".".equals(dir)) {
                // if dir is only "current" directory, just use its parent (== real current directory) as path
                relDir = databaseFile.get().getParent();
            } else {
                relDir = databaseFile.get().getParent() + File.separator + dir;
            }
            // If this directory actually exists, it is very likely that the
            // user wants us to use it:
            if (new File(relDir).exists()) {
                dir = relDir;
            }
        }
        return dir;
    }

    public DBMSSynchronizer getDBMSSynchronizer() {
        return this.dbmsSynchronizer;
    }

    public void clearDBMSSynchronizer() {
        this.dbmsSynchronizer = null;
    }

    public DatabaseLocation getLocation() {
        return this.location;
    }

    public void convertToSharedDatabase(Character keywordSeparator, GlobalBibtexKeyPattern globalCiteKeyPattern) {
        this.dbmsSynchronizer = new DBMSSynchronizer(this, keywordSeparator, globalCiteKeyPattern);
        this.database.registerListener(dbmsSynchronizer);
        this.metaData.registerListener(dbmsSynchronizer);

        this.location = DatabaseLocation.SHARED;
    }

    public void convertToLocalDatabase() {
        if (Objects.nonNull(dbmsSynchronizer) && (location == DatabaseLocation.SHARED)) {
            this.database.unregisterListener(dbmsSynchronizer);
            this.metaData.unregisterListener(dbmsSynchronizer);
        }

        this.location = DatabaseLocation.LOCAL;
    }
}
