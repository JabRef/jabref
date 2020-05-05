package org.jabref.model.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.database.event.CoarseChangeFilter;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.database.shared.DatabaseSynchronizer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

/**
 * Represents everything related to a BIB file. <p> The entries are stored in BibDatabase, the other data in MetaData
 * and the options relevant for this file in Defaults.
 */
public class BibDatabaseContext {

    private final BibDatabase database;
    private MetaData metaData;

    /**
     * The file where this database was last saved to.
     */
    private Optional<Path> file;

    private DatabaseSynchronizer dbmsSynchronizer;
    private CoarseChangeFilter dbmsListener;
    private DatabaseLocation location;

    public BibDatabaseContext() {
        this(new BibDatabase());
    }

    public BibDatabaseContext(BibDatabase database) {
        this(database, new MetaData());
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData) {
        this.database = Objects.requireNonNull(database);
        this.metaData = Objects.requireNonNull(metaData);
        this.location = DatabaseLocation.LOCAL;
        this.file = Optional.empty();
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, Path file) {
        this(database, metaData, file, DatabaseLocation.LOCAL);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, Path file, DatabaseLocation location) {
        this(database, metaData);
        Objects.requireNonNull(location);
        this.file = Optional.ofNullable(file);

        if (location == DatabaseLocation.LOCAL) {
            convertToLocalDatabase();
        }
    }

    public BibDatabaseMode getMode() {
        return metaData.getMode().orElse(BibDatabaseMode.BIBLATEX);
    }

    public void setMode(BibDatabaseMode bibDatabaseMode) {
        metaData.setMode(bibDatabaseMode);
    }

    public void setDatabasePath(Path file) {
        this.file = Optional.ofNullable(file);
    }

    /**
     * Get the file where this database was last saved to or loaded from, if any.
     *
     * @return Optional of the relevant File, or Optional.empty() if none is defined.
     */
    public Optional<Path> getDatabasePath() {
        return file;
    }

    public void clearDatabaseFile() {
        this.file = Optional.empty();
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

    /**
     * Look up the directories set up for this database.
     * There can be up to three directory definitions for these files: the database's
     * metadata can specify a general directory and/or a user-specific directory, or the preferences can specify one.
     * <p>
     * The settings are prioritized in the following order, and the first defined setting is used:
     * <ol>
     *     <li>user-specific metadata directory</li>
     *     <li>general metadata directory</li>
     *     <li>preferences directory</li>
     *     <li>BIB file directory</li>
     * </ol>
     *
     * @param preferences The fileDirectory preferences
     */
    public List<Path> getFileDirectoriesAsPaths(FilePreferences preferences) {
        List<Path> fileDirs = new ArrayList<>();

        // 1. Metadata user-specific directory
        metaData.getUserFileDirectory(preferences.getUser())
                .ifPresent(userFileDirectory -> fileDirs.add(getFileDirectoryPath(userFileDirectory)));

        // 2. Metadata general directory
        metaData.getDefaultFileDirectory()
                .ifPresent(metaDataDirectory -> fileDirs.add(getFileDirectoryPath(metaDataDirectory)));

        // 3. Preferences directory
        preferences.getFileDirectory().ifPresent(fileDirs::add);

        // 4. BIB file directory
        getDatabasePath().ifPresent(dbPath -> {
            Objects.requireNonNull(dbPath, "dbPath is null");
            Path parentPath = dbPath.getParent();
            if (parentPath == null) {
                parentPath = Path.of(System.getProperty("user.dir"));
            }
            Objects.requireNonNull(parentPath, "BibTeX database parent path is null");

            // Check if we should add it as primary file dir (first in the list) or not:
            if (preferences.isBibLocationAsPrimary()) {
                fileDirs.add(0, parentPath);
            } else {
                fileDirs.add(parentPath);
            }
        });

        return fileDirs.stream().map(Path::toAbsolutePath).collect(Collectors.toList());
    }

    /**
     * Returns the first existing file directory from  {@link #getFileDirectories(FilePreferences)}
     *
     * @param preferences The FilePreferences
     * @return Optional of Path
     */
    public Optional<Path> getFirstExistingFileDir(FilePreferences preferences) {
        return getFileDirectoriesAsPaths(preferences).stream().filter(Files::exists).findFirst();
    }

    /**
     * @deprecated use {@link #getFileDirectoriesAsPaths(FilePreferences)} instead
     */
    @Deprecated
    public List<String> getFileDirectories(FilePreferences preferences) {
        return getFileDirectoriesAsPaths(preferences).stream()
                                                     .map(directory -> directory.toAbsolutePath().toString())
                                                     .collect(Collectors.toList());
    }

    private Path getFileDirectoryPath(String directoryName) {
        Path directory = Path.of(directoryName);
        // If this directory is relative, we try to interpret it as relative to
        // the file path of this BIB file:
        Optional<Path> databaseFile = getDatabasePath();
        if (!directory.isAbsolute() && databaseFile.isPresent()) {
            return databaseFile.get().getParent().resolve(directory).normalize();
        }
        return directory;
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
