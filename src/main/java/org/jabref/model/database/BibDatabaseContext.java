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
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.crawler.StudyRepository;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.study.Study;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents everything related to a BIB file.
 *
 * <p> The entries are stored in BibDatabase, the other data in MetaData
 * and the options relevant for this file in Defaults.
 * </p>
 * <p>
 *     To get an instance for a .bib file, use {@link org.jabref.logic.importer.fileformat.BibtexParser}.
 * </p>
 */
@AllowedToUseLogic("because it needs access to shared database features")
public class BibDatabaseContext {

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
     * Returns whether this .bib file belongs to a {@link Study}
     */
    public boolean isStudy() {
        return this.getDatabasePath()
                .map(path -> path.getFileName().toString().equals(Crawler.FILENAME_STUDY_RESULT_BIB) &&
                        Files.exists(path.resolveSibling(StudyRepository.STUDY_DEFINITION_FILE_NAME)))
                .orElse(false);
    }

    /**
     * Look up the directories set up for this database.
     * There can be up to four directories definitions for these files:
     * <ol>
     * <li>next to the .bib file.</li>
     * <li>the preferences can specify a default one.</li>
     * <li>the database's metadata can specify a general directory.</li>
     * <li>the database's metadata can specify a user-specific directory.</li>
     * </ol>
     * <p>
     * The settings are prioritized in the following order, and the first defined setting is used:
     * <ol>
     *     <li>user-specific metadata directory</li>
     *     <li>general metadata directory</li>
     *     <li>BIB file directory (if configured in the preferences AND none of the two above directories are configured)</li>
     *     <li>preferences directory (if .bib file directory should not be used according to the preferences)</li>
     * </ol>
     *
     * @param preferences The fileDirectory preferences
     */
    public List<Path> getFileDirectories(FilePreferences preferences) {
        List<Path> fileDirs = new ArrayList<>();

        // 1. Metadata user-specific directory
        metaData.getUserFileDirectory(preferences.getUserAndHost())
                .ifPresent(userFileDirectory -> fileDirs.add(getFileDirectoryPath(userFileDirectory)));

        // 2. Metadata general directory
        metaData.getDefaultFileDirectory()
                .ifPresent(metaDataDirectory -> fileDirs.add(getFileDirectoryPath(metaDataDirectory)));

        // 3. BIB file directory or Main file directory
        // fileDirs.isEmpty in the case, 1) no user-specific file directory and 2) no general file directory is set
        // (in the metadata of the bib file)
        if (fileDirs.isEmpty() && preferences.shouldStoreFilesRelativeToBibFile()) {
            getDatabasePath().ifPresent(dbPath -> {
                Path parentPath = dbPath.getParent();
                if (parentPath == null) {
                    parentPath = Path.of(System.getProperty("user.dir"));
                }
                Objects.requireNonNull(parentPath, "BibTeX database parent path is null");
                fileDirs.add(parentPath);
            });
        } else {
            // Main file directory
            preferences.getMainFileDirectory().ifPresent(fileDirs::add);
        }

        return fileDirs.stream().map(Path::toAbsolutePath).collect(Collectors.toList());
    }

    /**
     * Returns the first existing file directory from  {@link #getFileDirectories(FilePreferences)}
     *
     * @return the path - or an empty optional, if none of the directories exists
     */
    public Optional<Path> getFirstExistingFileDir(FilePreferences preferences) {
        return getFileDirectories(preferences).stream()
                                              .filter(Files::exists)
                                              .findFirst();
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

    public Path getFulltextIndexPath() {
        Path appData = OS.getNativeDesktop().getFulltextIndexBaseDirectory();
        Path indexPath;

        if (getDatabasePath().isPresent()) {
            indexPath = appData.resolve(String.valueOf(this.getDatabasePath().get().hashCode()));
            LOGGER.debug("Index path for {} is {}", getDatabasePath().get(), indexPath);
            return indexPath;
        }

        indexPath = appData.resolve("unsaved");
        LOGGER.debug("Using index for unsaved database: {}", indexPath);
        return indexPath;
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
