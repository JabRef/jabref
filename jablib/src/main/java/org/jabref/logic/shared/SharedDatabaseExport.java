package org.jabref.logic.shared;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Read-only export of a shared SQL library into a plain `.bib` file, without a [DBMSSynchronizer]
/// and without a notification listener. This is what gives command line tools access to a shared
/// database: they read the exported file like any other library.
@NullMarked
public class SharedDatabaseExport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDatabaseExport.class);

    private SharedDatabaseExport() {
    }

    /// Connects to the shared database, writes its entries and meta data to a temporary file, and disconnects again.
    /// The file is deleted when the JVM exits.
    ///
    /// @return the path of the temporary `.bib` file
    /// @throws DatabaseNotSupportedException if the database is not a JabRef shared database
    public static Path pullToTemporaryBib(DBMSConnectionUrl url, CliPreferences preferences)
            throws SQLException, InvalidDBMSConnectionPropertiesException, DatabaseNotSupportedException, IOException {
        BibDatabaseContext context = pull(url, preferences);

        Path file = Files.createTempFile("jabref-shared-", ".bib");
        file.toFile().deleteOnExit();
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            new BibDatabaseWriter(writer, context, preferences).writeDatabase(context);
        }
        return file;
    }

    private static BibDatabaseContext pull(DBMSConnectionUrl url, CliPreferences preferences)
            throws SQLException, InvalidDBMSConnectionPropertiesException, DatabaseNotSupportedException {
        DBMSConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(url.type())
                .setHost(url.host())
                .setPort(url.port())
                .setDatabase(url.database())
                .setUser(url.user().orElse(""))
                .setPassword(url.password().orElse(""))
                .setUseSSL(url.useSSL())
                .setAllowPublicKeyRetrieval(true)
                // Parameters without a dedicated setting (e.g. sslmode=verify-full) survive only in the JDBC URL
                .setExpertMode(!url.query().isEmpty())
                .setJdbcUrl(url.toJdbcUrl())
                .createDBMSConnectionProperties();

        DBMSConnection connection = new DBMSConnection(properties);
        try {
            DBMSProcessor processor = new DBMSProcessor(connection);
            if (!processor.checkBaseIntegrity()) {
                throw new DatabaseNotSupportedException();
            }
            MetaData metaData = parseMetaData(processor.getSharedMetaData(), preferences);
            return new BibDatabaseContext(new BibDatabase(processor.getSharedEntries()), metaData);
        } finally {
            connection.getConnection().close();
        }
    }

    private static MetaData parseMetaData(Map<String, String> sharedMetaData, CliPreferences preferences) {
        MetaData metaData = new MetaData();
        try {
            // No file monitor: the exported file is written once and never watched
            new MetaDataParser(new DummyFileUpdateMonitor()).parse(
                    metaData,
                    sharedMetaData,
                    preferences.getBibEntryPreferences().getKeywordSeparator(),
                    preferences.getFilePreferences().getUserAndHost());
        } catch (ParseException e) {
            // Same as DBMSSynchronizer: unparsable meta data must not keep the entries from being read
            LOGGER.error("Could not parse the meta data of the shared library", e);
        }
        return metaData;
    }
}
