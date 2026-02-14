package org.jabref.model.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.UUID;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.crawler.StudyRepository;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Represents everything related to a BIB file.
///
/// The entries are stored in BibDatabase, the other data in MetaData
/// and the options relevant for this file in Defaults.
///
///
/// To get an instance for a .bib file, use {@link org.jabref.logic.importer.fileformat.BibtexParser}.
///
@AllowedToUseLogic("because it needs access to shared database features")
@NullMarked
public class BibDatabaseContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseContext.class);

    private final BibDatabase database;

    private MetaData metaData;

    /// Generate a random UID for unique of the concrete context
    /// In contrast to hashCode this stays unique
    private final String uid = "bibdatabasecontext_" + UUID.randomUUID();

    /// The path where this database was last saved to.
    @Nullable
    private Path path;

    @Nullable
    private DatabaseSynchronizer dbmsSynchronizer;

    @Nullable
    private CoarseChangeFilter dbmsListener;

    private DatabaseLocation location;

    public BibDatabaseContext() {
        this(new BibDatabase());
    }

    public BibDatabaseContext(BibDatabase database) {
        this(database, new MetaData());
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData) {
        this.database = database;
        this.metaData = metaData;
        this.location = DatabaseLocation.LOCAL;
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, Path path) {
        this(database, metaData, path, DatabaseLocation.LOCAL);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, @Nullable Path path, DatabaseLocation location) {
        this(database, metaData);
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

    public void setDatabasePath(@Nullable Path file) {
        this.path = file;
    }

    /// Get the path where this database was last saved to or loaded from, if any.
    ///
    /// @return Optional of the relevant Path, or Optional.empty() if none is defined.
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
        this.metaData = metaData;
    }

    public boolean isBiblatexMode() {
        return getMode() == BibDatabaseMode.BIBLATEX;
    }

    /// Returns whether this .bib file belongs to a {@link org.jabref.model.study.Study}
    public boolean isStudy() {
        return this.getDatabasePath()
                   .map(path -> Crawler.FILENAME_STUDY_RESULT_BIB.equals(path.getFileName().toString()) &&
                           Files.exists(path.resolveSibling(StudyRepository.STUDY_DEFINITION_FILE_NAME)))
                   .orElse(false);
    }

    /// Look up the directories set up for this database.
    /// There can be up to four directory definitions for these files:
    ///
    /// 1. next to the .bib file.
    /// 2. the preferences can specify a default one.
    /// 3. the database's metadata can specify a library-specific directory.
    /// 4. the database's metadata can specify a user-specific directory.
    ///
    ///
    /// The settings are prioritized in the following order, and the first defined setting is used:
    ///
    /// 1. user-specific metadata directory
    /// 2. general metadata directory
    /// 3. BIB file directory (if configured in the preferences AND none of the two above directories are configured)
    /// 4. preferences directory (if .bib file directory should not be used according to the (global) preferences)
    ///
    /// @param preferences The fileDirectory preferences
    /// @return List of existing absolute paths
    public List<Path> getFileDirectories(FilePreferences preferences) {
        SequencedSet<Path> result = new LinkedHashSet<>();
        FileDirectories directories = getAllFileDirectories(preferences);

        directories.userDirectory().ifPresent(result::add);
        directories.libraryDirectory().ifPresent(result::add);
        directories.fallbackDirectory().ifPresent(result::add);

        return new ArrayList<>(result);
    }

    /// Look up all configured file directories of this database.
    /// The returned record contains the following directories:
    ///
    /// 1. user-specific directory
    /// 2. library-specific directory
    /// 3. BIB file directory or Main file directory (depending on preferences)
    ///
    /// @param preferences The file directory preferences
    /// @return fixed-size record containing absolute paths (if configured)
    public FileDirectories getAllFileDirectories(FilePreferences preferences) {
        Optional<Path> userFileDirectory = metaData.getUserFileDirectory(preferences.getUserAndHost()).map(this::getFileDirectoryPath);
        Optional<Path> librarySpecificFileDirectory = metaData.getLibrarySpecificFileDirectory().map(this::getFileDirectoryPath);

        Optional<Path> bibOrMainFileDirectory;

        // BIB file directory or Main file directory (according to (global) preferences)
        if (preferences.shouldStoreFilesRelativeToBibFile()) {
            bibOrMainFileDirectory = getDatabasePath().map(dbPath -> {
                Path parentPath = dbPath.getParent();
                if (parentPath == null) {
                    parentPath = Path.of(System.getProperty("user.dir"));
                    LOGGER.warn("Parent path of database file {} is null. Falling back to {}.", dbPath, parentPath);
                }
                return parentPath.toAbsolutePath();
            });
        } else {
            bibOrMainFileDirectory = preferences.getMainFileDirectory();
        }

        return new FileDirectories(
                userFileDirectory,
                librarySpecificFileDirectory,
                bibOrMainFileDirectory
        );
    }

    /// Returns the first existing file directory from  {@link #getFileDirectories(FilePreferences)}
    ///
    /// @return the path - or an empty optional, if none of the directories exists
    public Optional<Path> getFirstExistingFileDir(FilePreferences preferences) {
        return getFileDirectories(preferences).stream()
                                              .filter(Files::exists)
                                              .findFirst();
    }

    /// @return The absolute path for the given directory
    private Path getFileDirectoryPath(String directory) {
        Path path = Path.of(directory);
        if (path.isAbsolute()) {
            return path;
        }

        // If this path is relative, we try to interpret it as relative to the file path of this BIB file:
        return getDatabasePath()
                .map(databaseFile -> Optional.ofNullable(databaseFile.getParent())
                                             .orElse(Path.of(""))
                                             .resolve(path)
                                             .normalize()
                                             .toAbsolutePath())
                .orElse(path);
    }

    public @Nullable DatabaseSynchronizer getDBMSSynchronizer() {
        return this.dbmsSynchronizer;
    }

    public void clearDBMSSynchronizer() {
        this.dbmsSynchronizer = null;
    }

    public DatabaseLocation getLocation() {
        return this.location;
    }

    public void convertToSharedDatabase(DatabaseSynchronizer dbmsSynchronizer) {
        this.dbmsSynchronizer = dbmsSynchronizer;

        this.dbmsListener = new CoarseChangeFilter(this);
        dbmsListener.registerListener(this.dbmsSynchronizer);

        this.location = DatabaseLocation.SHARED;
    }

    public void convertToLocalDatabase() {
        if (dbmsListener != null && (location == DatabaseLocation.SHARED)) {
            if (dbmsSynchronizer != null) {
                dbmsListener.unregisterListener(dbmsSynchronizer);
            }
            dbmsListener.shutdown();
        }

        this.location = DatabaseLocation.LOCAL;
    }

    public List<BibEntry> getEntries() {
        return database.getEntries();
    }

    /// @return The path to store the lucene index files. One directory for each library.
    public Path getFulltextIndexPath() {
        Path appData = Directories.getFulltextIndexBaseDirectory();
        Path indexPath;

        if (getDatabasePath().isPresent()) {
            Path databasePath = getDatabasePath().get();
            // Eventually, this leads to filenames as "40daf3b0--fuu.bib--2022-09-04--01.36.25.bib" --> "--" is used as separator between "groups"
            String fileName = BackupFileUtil.getUniqueFilePrefix(databasePath) + "--" + databasePath.getFileName();
            indexPath = appData.resolve(fileName);
            LOGGER.debug("Index path for {} is {}", getDatabasePath().get(), indexPath);
            return indexPath;
        }

        indexPath = appData.resolve("unsaved");
        LOGGER.debug("Using index for unsaved database: {}", indexPath);
        return indexPath;
    }

    public static BibDatabaseContext of(Reader bibContentReader, ImportFormatPreferences importFormatPreferences) throws JabRefException {
        BibtexParser parser = new BibtexParser(importFormatPreferences);
        try {
            ParserResult result = parser.parse(bibContentReader);
            return result.getDatabaseContext();
        } catch (IOException e) {
            throw new JabRefException("Failed to parse BibTeX", e);
        }
    }

    public static BibDatabaseContext of(String bibContent, ImportFormatPreferences importFormatPreferences) throws JabRefException {
        return of(Reader.of(bibContent), importFormatPreferences);
    }

    public static BibDatabaseContext of(InputStream bibContentStream, ImportFormatPreferences importFormatPreferences) throws JabRefException {
        try (Reader reader = new BufferedReader(new InputStreamReader(bibContentStream))) {
            return of(reader, importFormatPreferences);
        } catch (IOException e) {
            throw new JabRefException("Failed to close stream", e);
        }
    }

    public static BibDatabaseContext empty() {
        return new BibDatabaseContext(new BibDatabase(), new MetaData());
    }

    @Override
    public String toString() {
        return "BibDatabaseContext{" +
                "metaData=" + metaData +
                ", mode=" + getMode() +
                ", databasePath=" + getDatabasePath() +
                ", biblatexMode=" + isBiblatexMode() +
                ", uid= " + getUid() +
                ", fulltextIndexPath=" + getFulltextIndexPath() +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BibDatabaseContext that)) {
            return false;
        }
        return Objects.equals(database, that.database) && Objects.equals(metaData, that.metaData) && Objects.equals(path, that.path) && location == that.location;
    }

    /// @implNote This implementation needs to be consistent with equals. That means, as soon as a new entry is added to the database, two different instances of BibDatabaseContext are not equal - and thus, the hashCode also needs to change. This has the drawback, that one cannot create HashMaps from the BiDatabaseContext anymore, as the hashCode changes as soon as a new entry is added.
    @Override
    public int hashCode() {
        return Objects.hash(database, metaData, path, location);
    }

    /// Get the generated UID for the current context. Can be used to distinguish contexts with changing metadata etc
    ///
    /// This is required, because of {@link #hashCode()} implementation.
    ///
    /// @return The generated UID in UUIDv4 format with the prefix bibdatabasecontext_
    public String getUid() {
        return uid;
    }
}
