package org.jabref.model.database;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.Defaults;
import org.jabref.model.database.event.CoarseChangeFilter;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.database.shared.DatabaseSynchronizer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.metadata.MetaData;

/**
 * Represents everything related to a BIB file. <p> The entries are stored in BibDatabase, the other data in MetaData
 * and the options relevant for this file in Defaults.
 */
public class BibDatabaseContext {

    private final BibDatabase database;
    private final Defaults defaults;
    private MetaData metaData;
    /**
     * The file where this database was last saved to.
     */
    private File file;
    private DatabaseSynchronizer dbmsSynchronizer;
    private CoarseChangeFilter dbmsListener;
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

    public BibDatabaseContext(BibDatabase database, MetaData metaData, File file, Defaults defaults,
                              DatabaseLocation location) {
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

    public BibDatabaseMode getMode() {
        Optional<BibDatabaseMode> mode = metaData.getMode();

        if (!mode.isPresent()) {
            BibDatabaseMode inferredMode = BibDatabaseModeDetection.inferMode(database);
            BibDatabaseMode newMode = BibDatabaseMode.BIBTEX;
            if ((defaults.mode == BibDatabaseMode.BIBLATEX) || (inferredMode == BibDatabaseMode.BIBLATEX)) {
                newMode = BibDatabaseMode.BIBLATEX;
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
     * @deprecated use {@link #getDatabasePath()} instead
     */
    @Deprecated
    public Optional<File> getDatabaseFile() {
        return Optional.ofNullable(file);
    }

    public void setDatabaseFile(File file) {
        this.file = file;
    }

    public Optional<Path> getDatabasePath() {
        return Optional.ofNullable(file).map(File::toPath);
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

    public List<Path> getFileDirectoriesAsPaths(FileDirectoryPreferences preferences) {
        // Filter for empty string, as this would be expanded to the jar-directory with Paths.get()
        return getFileDirectories(preferences).stream()
                .filter(s -> !s.isEmpty())
                .map(Paths::get)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated use {@link #getFileDirectoriesAsPaths(FileDirectoryPreferences)} instead
     */
    @Deprecated
    public List<String> getFileDirectories(FileDirectoryPreferences preferences) {
        return getFileDirectories(FieldName.FILE, preferences);
    }

    /**
     * Returns the first existing file directory from  {@link #getFileDirectories(FileDirectoryPreferences)}
     *
     * @param preferences The FileDirectoryPreferences
     * @return Optional of Path
     */
    public Optional<Path> getFirstExistingFileDir(FileDirectoryPreferences preferences) {
        return getFileDirectoriesAsPaths(preferences).stream().filter(Files::exists).findFirst();
    }

    /**
     * Look up the directories set up for the given field type for this database. If no directory is set up, return that
     * defined in global preferences. There can be up to three directory definitions for these files: the database's
     * metadata can specify a general directory and/or a user-specific directory or the preferences can specify one. <p>
     * The settings are prioritized in the following order and the first defined setting is used:
     * <ol>
     *     <li>metdata</li>
     *     <li>user-specific directory</li>
     *     <li>preferences directory</li>
     *     <li>BIB file directory</li>
     * </ol>
     *
     * @param fieldName   The field type
     * @param preferences The fileDirectory preferences
     * @return The default directory for this field type.
     */
    public List<String> getFileDirectories(String fieldName, FileDirectoryPreferences preferences) {
        List<String> fileDirs = new ArrayList<>();

        // 1. metadata user-specific directory
        Optional<String> userFileDirectory = metaData.getUserFileDirectory(preferences.getUser());
        if (userFileDirectory.isPresent()) {
            fileDirs.add(getFileDirectoryPath(userFileDirectory.get()));
        }

        // 2. metadata general directory
        Optional<String> metaDataDirectory = metaData.getDefaultFileDirectory();
        if (metaDataDirectory.isPresent()) {
            fileDirs.add(getFileDirectoryPath(metaDataDirectory.get()));
        }

        // 3. preferences directory
        preferences.getFileDirectory(fieldName).ifPresent(path -> fileDirs.add(path.toAbsolutePath().toString()));

        // 4. BIB file directory
        getDatabasePath().ifPresent(dbPath -> {
            Objects.requireNonNull(dbPath, "dbPath is null");
            Path parentPath = dbPath.getParent();
            if (parentPath == null) {
                parentPath = Paths.get(System.getProperty("user.dir"));
            }
            Objects.requireNonNull(parentPath, "BibTeX database parent path is null");
            String parentDir = parentPath.toAbsolutePath().toString();
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

    public DatabaseSynchronizer getDBMSSynchronizer() {
        return this.dbmsSynchronizer;
    }

    public void clearDBMSSynchronizer() {
        this.dbmsSynchronizer = null;
    }

    public DatabaseLocation getLocation() {
        return this.location;
    }

    public void convertToSharedDatabase(DatabaseSynchronizer dmbsSynchronizer) {
        this.dbmsSynchronizer = dmbsSynchronizer;

        this.dbmsListener = new CoarseChangeFilter(this);
        dbmsListener.registerListener(dbmsSynchronizer);

        this.location = DatabaseLocation.SHARED;
    }

    @Override
    public String toString() {
        return "BibDatabaseContext{" +
                "file=" + file +
                ", location=" + location +
                '}';
    }

    public void convertToLocalDatabase() {
        if (Objects.nonNull(dbmsListener) && (location == DatabaseLocation.SHARED)) {
            dbmsListener.unregisterListener(dbmsSynchronizer);
            dbmsListener.shutdown();
        }

        this.location = DatabaseLocation.LOCAL;
    }

    public List<BibEntry> getEntries() {
        return database.getEntries();
    }
}
