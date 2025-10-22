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
import org.jabref.model.study.Study;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents everything related to a BIB file.
 *
 * <p> The entries are stored in BibDatabase, the other data in MetaData
 * and the options relevant for this file in Defaults.
 * </p>
 * <p>
 * To get an instance for a .bib file, use {@link org.jabref.logic.importer.fileformat.BibtexParser}.
 * Alternatively, use the {@link Builder} for flexible construction:
 * </p>
 * <pre>{@code
 * BibDatabaseContext context = BibDatabaseContext.builder()
 *     .database(database)
 *     .metaData(metaData)
 *     .databasePath(path)
 *     .location(DatabaseLocation.LOCAL)
 *     .build();
 * }</pre>
 */
@AllowedToUseLogic("because it needs access to shared database features")
public class BibDatabaseContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseContext.class);

    private final BibDatabase database;
    private MetaData metaData;

    /**
     * Generate a random UID for unique of the concrete context
     * In contrast to hashCode this stays unique
     */
    private final String uid = "bibdatabasecontext_" + UUID.randomUUID();

    /**
     * The path where this database was last saved to.
     */
    private Path path;

    private DatabaseSynchronizer dbmsSynchronizer;
    private CoarseChangeFilter dbmsListener;
    private DatabaseLocation location;

    /**
     * Creates a new BibDatabaseContext with an empty database.
     */
    public BibDatabaseContext() {
        this(new BibDatabase());
    }

    /**
     * Creates a new BibDatabaseContext with the given database.
     *
     * @param database the BibDatabase (must not be null)
     */
    public BibDatabaseContext(@NonNull BibDatabase database) {
        this(database, new MetaData());
    }

    /**
     * Creates a new BibDatabaseContext with the given database and metadata.
     *
     * @param database the BibDatabase (must not be null)
     * @param metaData the MetaData (must not be null)
     */
    public BibDatabaseContext(@NonNull BibDatabase database, @NonNull MetaData metaData) {
        this.database = database;
        this.metaData = metaData;
        this.location = DatabaseLocation.LOCAL;
    }

    /**
     * Creates a new BibDatabaseContext with the given database, metadata, and path.
     *
     * @param database the BibDatabase (must not be null)
     * @param metaData the MetaData (must not be null)
     * @param path     the file path where the database is located (can be null)
     */
    public BibDatabaseContext(@NonNull BibDatabase database, @NonNull MetaData metaData, Path path) {
        this(database, metaData, path, DatabaseLocation.LOCAL);
    }

    /**
     * Creates a new BibDatabaseContext with the given database, metadata, path, and location.
     *
     * @param database the BibDatabase (must not be null)
     * @param metaData the MetaData (must not be null)
     * @param path     the file path where the database is located (can be null)
     * @param location the database location type (must not be null)
     */
    public BibDatabaseContext(@NonNull BibDatabase database, @NonNull MetaData metaData, Path path, @NonNull DatabaseLocation location) {
        this(database, metaData);
        this.path = path;

        if (location == DatabaseLocation.LOCAL) {
            convertToLocalDatabase();
        }
    }

    /**
     * Private constructor for use by the Builder.
     *
     * @param builder the Builder containing all configuration parameters
     */
    private BibDatabaseContext(Builder builder) {
        this.database = builder.database;
        this.metaData = builder.metaData;
        this.path = builder.path;
        this.location = builder.location;

        if (this.location == DatabaseLocation.LOCAL) {
            convertToLocalDatabase();
        }
    }

    /**
     * Creates a new Builder for constructing BibDatabaseContext instances.
     *
     * @return a new Builder instance with default values
     */
    public static Builder builder() {
        return new Builder();
    }

    public BibDatabaseMode getMode() {
        return metaData.getMode().orElse(BibDatabaseMode.BIBLATEX);
    }

    public void setMode(@NonNull BibDatabaseMode bibDatabaseMode) {
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

    public void setMetaData(@NonNull MetaData metaData) {
        this.metaData = metaData;
    }

    public boolean isBiblatexMode() {
        return getMode() == BibDatabaseMode.BIBLATEX;
    }

    /**
     * Returns whether this .bib file belongs to a {@link Study}
     */
    public boolean isStudy() {
        return this.getDatabasePath()
                   .map(path -> Crawler.FILENAME_STUDY_RESULT_BIB.equals(path.getFileName().toString()) &&
                           Files.exists(path.resolveSibling(StudyRepository.STUDY_DEFINITION_FILE_NAME)))
                   .orElse(false);
    }

    /**
     * Look up the directories set up for this database.
     * There can be up to four directories definitions for these files:
     * <ol>
     * <li>next to the .bib file.</li>
     * <li>the preferences can specify a default one.</li>
     * <li>the database's metadata can specify a library-specific directory.</li>
     * <li>the database's metadata can specify a user-specific directory.</li>
     * </ol>
     * <p>
     * The settings are prioritized in the following order, and the first defined setting is used:
     * <ol>
     *     <li>user-specific metadata directory</li>
     *     <li>general metadata directory</li>
     *     <li>BIB file directory (if configured in the preferences AND none of the two above directories are configured)</li>
     *     <li>preferences directory (if .bib file directory should not be used according to the (global) preferences)</li>
     * </ol>
     *
     * @param preferences The fileDirectory preferences
     * @return List of existing absolute paths
     */
    public List<Path> getFileDirectories(FilePreferences preferences) {
        // Paths are a) ordered and b) should be contained only once in the result
        LinkedHashSet<Path> fileDirs = new LinkedHashSet<>(3);

        Optional<Path> userFileDirectory = metaData.getUserFileDirectory(preferences.getUserAndHost()).map(this::getFileDirectoryPath);
        userFileDirectory.ifPresent(fileDirs::add);

        Optional<Path> librarySpecificFileDirectory = metaData.getLibrarySpecificFileDirectory().map(this::getFileDirectoryPath);
        librarySpecificFileDirectory.ifPresent(fileDirs::add);

        // fileDirs.isEmpty() is true after these two if there are no directories set in the BIB file itself:
        //   1) no user-specific file directory set (in the metadata of the bib file) and
        //   2) no library-specific file directory is set (in the metadata of the bib file)

        // BIB file directory or main file directory (according to (global) preferences)
        if (preferences.shouldStoreFilesRelativeToBibFile()) {
            getDatabasePath().ifPresent(dbPath -> {
                Path parentPath = dbPath.getParent();
                if (parentPath == null) {
                    parentPath = Path.of(System.getProperty("user.dir"));
                    LOGGER.warn("Parent path of database file {} is null. Falling back to {}.", dbPath, parentPath);
                }
                Objects.requireNonNull(parentPath, "BibTeX database parent path is null");
                fileDirs.add(parentPath.toAbsolutePath());
            });
        } else {
            preferences.getMainFileDirectory()
                       .filter(path -> !fileDirs.contains(path))
                       .ifPresent(fileDirs::add);
        }

        return new ArrayList<>(fileDirs);
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

    /**
     * @return The absolute path for the given directory
     */
    private Path getFileDirectoryPath(String directory) {
        Path path = Path.of(directory);
        if (path.isAbsolute()) {
            return path;
        }

        // If this path is relative, we try to interpret it as relative to the file path of this BIB file:
        return getDatabasePath()
                .map(databaseFile -> databaseFile.getParent().resolve(path).normalize().toAbsolutePath())
                .orElse(path);
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
        if (dbmsListener != null && (location == DatabaseLocation.SHARED)) {
            dbmsListener.unregisterListener(dbmsSynchronizer);
            dbmsListener.shutdown();
        }

        this.location = DatabaseLocation.LOCAL;
    }

    public List<BibEntry> getEntries() {
        return database.getEntries();
    }

    /**
     * @return The path to store the lucene index files. One directory for each library.
     */
    public @NonNull Path getFulltextIndexPath() {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BibDatabaseContext that)) {
            return false;
        }
        return Objects.equals(database, that.database) && Objects.equals(metaData, that.metaData) && Objects.equals(path, that.path) && location == that.location;
    }

    /**
     * @implNote This implementation needs to be consistent with equals. That means, as soon as a new entry is added to the database, two different instances of BibDatabaseContext are not equal - and thus, the hashCode also needs to change. This has the drawback, that one cannot create HashMaps from the BiDatabaseContext anymore, as the hashCode changes as soon as a new entry is added.
     */
    @Override
    public int hashCode() {
        return Objects.hash(database, metaData, path, location);
    }

    /**
     * Get the generated UID for the current context. Can be used to distinguish contexts with changing metadata etc
     * <p>
     * This is required, because of {@link #hashCode()} implementation.
     *
     * @return The generated UID in UUIDv4 format with the prefix bibdatabasecontext_
     */
    public String getUid() {
        return uid;
    }

    /**
     * Builder for creating {@link BibDatabaseContext} instances with a fluent API.
     * Provides flexible configuration of database context properties.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * BibDatabaseContext context = BibDatabaseContext.builder()
     *     .database(myDatabase)
     *     .metaData(myMetaData)
     *     .databasePath(Paths.get("my-library.bib"))
     *     .location(DatabaseLocation.LOCAL)
     *     .build();
     * }</pre>
     *
     * <p>For simple cases, the default constructors can still be used:</p>
     * <pre>{@code
     * BibDatabaseContext context = new BibDatabaseContext();
     * BibDatabaseContext context = new BibDatabaseContext(database);
     * }</pre>
     */
    public static class Builder {
        // Optional parameters with default values
        private BibDatabase database = new BibDatabase();
        private MetaData metaData = new MetaData();
        private Path path = null;
        private DatabaseLocation location = DatabaseLocation.LOCAL;

        /**
         * Creates a new Builder with default values.
         */
        public Builder() {
            // Default constructor with default values initialized above
        }

        /**
         * Sets the BibDatabase for this context.
         *
         * @param database the BibDatabase (must not be null)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if database is null
         */
        public Builder database(@NonNull BibDatabase database) {
            if (database == null) {
                throw new IllegalArgumentException("database cannot be null");
            }
            this.database = database;
            return this;
        }

        /**
         * Sets the MetaData for this context.
         *
         * @param metaData the MetaData (must not be null)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if metaData is null
         */
        public Builder metaData(@NonNull MetaData metaData) {
            if (metaData == null) {
                throw new IllegalArgumentException("metaData cannot be null");
            }
            this.metaData = metaData;
            return this;
        }

        /**
         * Sets the file path where the database is located.
         *
         * @param path the file path (can be null for unsaved databases)
         * @return this Builder instance for method chaining
         */
        public Builder databasePath(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the database location type (local or shared).
         *
         * @param location the DatabaseLocation (must not be null)
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if location is null
         */
        public Builder location(@NonNull DatabaseLocation location) {
            if (location == null) {
                throw new IllegalArgumentException("location cannot be null");
            }
            this.location = location;
            return this;
        }

        /**
         * Builds and returns a new {@link BibDatabaseContext} instance
         * with the configured parameters.
         *
         * @return a new BibDatabaseContext instance
         */
        public BibDatabaseContext build() {
            return new BibDatabaseContext(this);
        }
    }
}
