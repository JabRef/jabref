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

import org.jabref.model.database.event.CoarseChangeFilter;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.database.shared.DatabaseSynchronizer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
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

    /**
     * Get the file where this database was last saved to or loaded from, if any.
     *
     * @return Optional of the relevant File, or Optional.empty() if none is defined.
     * @deprecated use {@link #getDatabasePath()} instead
     */
    @Deprecated
    public Optional<File> getDatabaseFile() {
        return file.map(Path::toFile);
    }

    /**
     * @param file the database file
     * @deprecated use {@link #setDatabaseFile(Path)}
     */
    @Deprecated
    public void setDatabaseFile(File file) {
        this.file = Optional.ofNullable(file).map(File::toPath);
    }

    public void setDatabaseFile(Path file) {
        this.file = Optional.ofNullable(file);
    }

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

    public List<Path> getFileDirectoriesAsPaths(FilePreferences preferences) {
        // Filter for empty string, as this would be expanded to the jar-directory with Paths.get()
        return getFileDirectories(preferences).stream()
                                              .filter(s -> !s.isEmpty())
                                              .map(Paths::get)
                                              .map(Path::toAbsolutePath)
                                              .map(Path::normalize)
                                              .collect(Collectors.toList());
    }

    /**
     * @deprecated use {@link #getFileDirectoriesAsPaths(FilePreferences)} instead
     */
    @Deprecated
    public List<String> getFileDirectories(FilePreferences preferences) {
        return getFileDirectories(StandardField.FILE, preferences);
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
     * Look up the directories set up for the given field type for this database. If no directory is set up, return that
     * defined in global preferences. There can be up to three directory definitions for these files: the database's
     * metadata can specify a general directory and/or a user-specific directory or the preferences can specify one. <p>
     * The settings are prioritized in the following order and the first defined setting is used:
     * <ol>
     *     <li>metadata</li>
     *     <li>user-specific directory</li>
     *     <li>preferences directory</li>
     *     <li>BIB file directory</li>
     * </ol>
     *
     * @param field       The field
     * @param preferences The fileDirectory preferences
     * @return The default directory for this field type.
     */
    public List<String> getFileDirectories(Field field, FilePreferences preferences) {
        List<String> fileDirs = new ArrayList<>();

        // 1. metadata user-specific directory
        metaData.getUserFileDirectory(preferences.getUser())
                .ifPresent(userFileDirectory -> fileDirs.add(getFileDirectoryPath(userFileDirectory)));

        // 2. metadata general directory
        metaData.getDefaultFileDirectory()
                .ifPresent(metaDataDirectory -> fileDirs.add(getFileDirectoryPath(metaDataDirectory)));

        // 3. preferences directory
        preferences.getFileDirectory(field).ifPresent(path -> fileDirs.add(path.toAbsolutePath().toString()));

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
