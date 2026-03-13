package org.jabref.logic.journals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.jabref.logic.journals.ltwa.LtwaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///
/// This class loads abbreviations and populates the Postgres database for use by {@link JournalAbbreviationRepository}.
///
/// Built-in abbreviations are loaded from a bundled SQL file (`journal-list.sql`) into Postgres.
/// Custom abbreviations from external CSV files are loaded in memory via {@link JournalAbbreviationRepository#addCustomAbbreviations}.
///
/// Abbreviations are available at <a href="https://github.com/JabRef/abbrv.jabref.org/">https://github.com/JabRef/abbrv.jabref.org/</a>.
///
public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    public static Collection<Abbreviation> readAbbreviationsFromCsvFile(Path file) throws IOException {
        LOGGER.debug("Reading journal list from file {}", file);
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    public static JournalAbbreviationRepository loadRepository(JournalAbbreviationPreferences journalAbbreviationPreferences,
                                                               DataSource dataSource) {
        JournalAbbreviationRepository repository;

        // Initialize built-in abbreviations in Postgres
        try {
            populateDatabase(dataSource);
            LtwaRepository ltwaRepository = loadLtwaRepository();
            repository = new JournalAbbreviationRepository(dataSource, ltwaRepository);
            LOGGER.debug("Loaded journal abbreviations from Postgres");
        } catch (IOException | SQLException e) {
            LOGGER.error("Error while loading journal abbreviation repository", e);
            return null;
        }

        // Read external lists
        List<String> lists = journalAbbreviationPreferences.getExternalJournalLists();
        if (lists != null && !lists.isEmpty()) {
            // reversing ensures that the latest lists overwrites the former one
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    repository.addCustomAbbreviations(readAbbreviationsFromCsvFile(Path.of(filename)));
                } catch (IOException | InvalidPathException e) {
                    // invalid path might come from unix/windows mixup of prefs
                    LOGGER.error("Cannot read external journal list file {}", filename, e);
                }
            }
        }
        return repository;
    }

    /// Populates the Postgres database with the built-in journal abbreviation list.
    /// The SQL file is bundled as a resource and contains CREATE TABLE + INSERT statements.
    /// Uses `ON CONFLICT` so it is safe to call multiple times (idempotent).
    private static void populateDatabase(DataSource dataSource) throws IOException, SQLException {
        // Skip if table already exists — safe for parallel test execution
        try (Connection conn = dataSource.getConnection()) {
            try (var tables = conn.getMetaData().getTables(null, null, "journal_abbreviation", null)) {
                if (tables.next()) {
                    LOGGER.debug("Journal abbreviation table already exists, skipping population");
                    return;
                }
            }
        }
        try (InputStream resourceAsStream = JournalAbbreviationLoader.class.getResourceAsStream("/journals/journal-list.sql")) {
            if (resourceAsStream == null) {
                LOGGER.warn("There is no journal-list.sql. Skipping built-in abbreviation loading.");
                return;
            }
            String sql = new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
            LOGGER.debug("Populated journal abbreviation table from journal-list.sql");
        }
    }

    private static LtwaRepository loadLtwaRepository() throws IOException {
        try (InputStream resourceAsStream = JournalAbbreviationLoader.class.getResourceAsStream("/journals/ltwa-list.mv")) {
            if (resourceAsStream == null) {
                LOGGER.warn("There is no ltwa-list.mv. We cannot load the LTWA repository.");
                throw new IOException("LTWA repository not found");
            } else {
                Path tempDir = Files.createTempDirectory("jabref-ltwa");
                Path tempLtwaList = tempDir.resolve("ltwa-list.mv");
                Files.copy(resourceAsStream, tempLtwaList);
                LtwaRepository ltwaRepository = new LtwaRepository(tempLtwaList);
                tempDir.toFile().deleteOnExit();
                tempLtwaList.toFile().deleteOnExit();
                return ltwaRepository;
            }
        }
    }

    /// Loads the built-in repository using the given data source. Used for testing.
    public static JournalAbbreviationRepository loadBuiltInRepository(DataSource dataSource) {
        return loadRepository(new JournalAbbreviationPreferences(List.of(), true), dataSource);
    }
}
