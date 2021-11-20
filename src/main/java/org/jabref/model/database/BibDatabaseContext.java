package org.jabref.model.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.gui.LibraryTab;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.pdf.search.SearchFieldConstants;
import org.jabref.preferences.FilePreferences;

import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents everything related to a BIB file. <p> The entries are stored in BibDatabase, the other data in MetaData
 * and the options relevant for this file in Defaults.
 */
@AllowedToUseLogic("because it needs access to shared database features")
public class BibDatabaseContext {

    public static final String SEARCH_INDEX_BASE_PATH = "JabRef";

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final BibDatabase database;
    private MetaData metaData;

    /**
     * The path where this database was last saved to.
     */
    private Path path;

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
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, Path path) {
        this(database, metaData, path, DatabaseLocation.LOCAL);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, Path path, DatabaseLocation location) {
        this(database, metaData);
        Objects.requireNonNull(location);
        this.path = path;

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
        this.path = file;
    }

    /**
     * Get the path where this database was last saved to or loaded from, if any.
     *
     * @return Optional of the relevant Path, or Optional.empty() if none is defined.
     */
    public Optional<Path> getDatabasePath() {
        return Optional.ofNullable(path);
    }

    public void clearDatabasePath() {
        this.path = null;
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
    public List<Path> getFileDirectories(FilePreferences preferences) {
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
        if (preferences.shouldStoreFilesRelativeToBibFile()) {
            getDatabasePath().ifPresent(dbPath -> {
                Path parentPath = dbPath.getParent();
                if (parentPath == null) {
                    parentPath = Path.of(System.getProperty("user.dir"));
                }
                Objects.requireNonNull(parentPath, "BibTeX database parent path is null");
                fileDirs.add(parentPath);
            });
        }

        return fileDirs.stream().map(Path::toAbsolutePath).collect(Collectors.toList());
    }

    /**
     * Returns the first existing file directory from  {@link #getFileDirectories(FilePreferences)}
     *
     * @param preferences The FilePreferences
     * @return Optional of Path
     */
    public Optional<Path> getFirstExistingFileDir(FilePreferences preferences) {
        return getFileDirectories(preferences).stream().filter(Files::exists).findFirst();
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

    /**
     * check if the database has any empty entries
     * @return true if the database has any empty entries; otherwise false
     */
    public boolean hasEmptyEntries() {
        return this.getEntries().stream().anyMatch(entry->entry.getFields().isEmpty());
    }

    public static Path getFulltextIndexBasePath() {
        return Path.of(AppDirsFactory.getInstance().getUserDataDir(SEARCH_INDEX_BASE_PATH, SearchFieldConstants.VERSION, "org.jabref"));
    }

    public Path getFulltextIndexPath() {
        Path appData = getFulltextIndexBasePath();

        if (getDatabasePath().isPresent()) {
            LOGGER.info("Index path for {} is {}", getDatabasePath().get(), appData);
            return appData.resolve(String.valueOf(this.getDatabasePath().get().hashCode()));
        }

        return appData.resolve("unsaved");
    }

    @Override
    public String toString() {
        return "BibDatabaseContext{" +
                "metaData=" + metaData +
                ", mode=" + getMode() +
                ", databasePath=" + getDatabasePath() +
                ", biblatexMode=" + isBiblatexMode() +
                ", fulltextIndexPath=" + getFulltextIndexPath() +
                '}';
    }
}
