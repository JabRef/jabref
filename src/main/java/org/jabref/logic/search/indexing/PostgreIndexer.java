package org.jabref.logic.search.indexing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.SearchFieldConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgreIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreIndexer.class);
    private static final String SPLIT_VALUES_PREFIX = "_split_values";
    private static int NUMBER_OF_UNSAVED_LIBRARIES = 1;

    private final BibDatabaseContext databaseContext;
    private final Connection connection;
    private final String libraryName;
    private final String tableName;

    public PostgreIndexer(BibDatabaseContext databaseContext, Connection connection) {
        this.databaseContext = databaseContext;
        this.connection = connection;
        this.libraryName = databaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElse("unsaved");
        if ("unsaved".equals(databaseContext.getPostgreTableName())) {
            this.tableName = "unsaved" + NUMBER_OF_UNSAVED_LIBRARIES++;
        } else {
            this.tableName = databaseContext.getPostgreTableName();
        }
        setup();
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * Creates a table for the library in the database, and sets up indexes on the columns.
     */
    private void setup() {
        try {
            connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS "%s" (
                        %s TEXT NOT NULL,
                        %s TEXT NOT NULL,
                        %s TEXT,
                        %s TEXT,
                        PRIMARY KEY (%s, %s)
                    )
                    """.formatted(tableName,
                    PostgreConstants.ENTRY_ID,
                    PostgreConstants.FIELD_NAME,
                    PostgreConstants.FIELD_VALUE_LITERAL,
                    PostgreConstants.FIELD_VALUE_TRANSFORMED,
                    PostgreConstants.ENTRY_ID,
                    PostgreConstants.FIELD_NAME));

            String tableNameSplitValues = tableName + "_split_values";
            connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS "%s" (
                        %s TEXT NOT NULL,
                        %s TEXT NOT NULL,
                        %s TEXT
                    )
                    """.formatted(tableNameSplitValues,
                    PostgreConstants.ENTRY_ID,
                    PostgreConstants.FIELD_NAME,
                    PostgreConstants.FIELD_SPLIT_VALUE));

            LOGGER.debug("Created tables for library: {}", libraryName);

            // btree index on id column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s" ON "%s" ("%s")
                    """.formatted(PostgreConstants.ENTRY_ID.getIndexName(tableName), tableName, PostgreConstants.ENTRY_ID));

            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s" ON "%s" ("%s")
                    """.formatted(PostgreConstants.ENTRY_ID.getIndexName(tableNameSplitValues), tableName, PostgreConstants.ENTRY_ID));

            // btree index on field name column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s" ON "%s" ("%s")
                    """.formatted(PostgreConstants.FIELD_NAME.getIndexName(tableName), tableName, PostgreConstants.FIELD_NAME));

            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s" ON "%s" ("%s")
                    """.formatted(PostgreConstants.FIELD_NAME.getIndexName(tableNameSplitValues), tableName, PostgreConstants.FIELD_NAME));

            // btree index on spilt values column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s" ON "%s" ("%s")
                    """.formatted(PostgreConstants.FIELD_SPLIT_VALUE.getIndexName(tableNameSplitValues), tableName, PostgreConstants.FIELD_SPLIT_VALUE));

            // trigram index on field value column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s" ON "%s" USING gin ("%s" gin_trgm_ops)
                    """.formatted(PostgreConstants.FIELD_VALUE_LITERAL.getIndexName(tableName), tableName, PostgreConstants.FIELD_VALUE_LITERAL));

            // trigram index on field value column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s" ON "%s" USING gin ("%s" gin_trgm_ops)
                    """.formatted(PostgreConstants.FIELD_VALUE_TRANSFORMED.getIndexName(tableName), tableName, PostgreConstants.FIELD_VALUE_TRANSFORMED));

            LOGGER.debug("Created indexes for library: {}", libraryName);
        } catch (SQLException e) {
            LOGGER.error("Could not create table for library: {}", libraryName, e);
        }
    }

    public void updateOnStart(BackgroundTask<?> task) {
        addToIndex(databaseContext.getDatabase().getEntries(), task);
    }

    public void addToIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
        task.setTitle(Localization.lang("Indexing bib fields for %0", libraryName));
        int i = 1;
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Adding {} entries to index", entries.size());
        for (BibEntry entry : entries) {
            if (task.isCancelled()) {
                LOGGER.debug("Indexing canceled");
                return;
            }
            addToIndex(entry);
            task.updateProgress(i, entries.size());
            task.updateMessage(Localization.lang("%0 of %1 entries added to the index.", i, entries.size()));
            i++;
        }
        LOGGER.debug("Added {} entries to index in {} ms", entries.size(), System.currentTimeMillis() - startTime);
    }

    private void addToIndex(BibEntry bibEntry) {
        String insertFieldQuery = """
            INSERT INTO "%s" ("%s", "%s", "%s", "%s")
            VALUES (?, ?, ?, ?)
            """.formatted(tableName,
                PostgreConstants.ENTRY_ID,
                PostgreConstants.FIELD_NAME,
                PostgreConstants.FIELD_VALUE_LITERAL,
                PostgreConstants.FIELD_VALUE_TRANSFORMED);

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertFieldQuery)) {
            String entryId = bibEntry.getId();
            for (Map.Entry<Field, String> field : bibEntry.getFieldMap().entrySet()) {
                preparedStatement.setString(1, entryId);
                preparedStatement.setString(2, field.getKey().getName());
                preparedStatement.setString(3, field.getValue());

                // If a field exists, there also exists a resolved field latex free.
                // We add a `.orElse("")` only because there could be some flaw in the future in the code - and we want to have search working even if the flaws are present.
                // To uncover these flaws, we add the "assert" statement.
                // One potential future flaw is that the bibEntry is modified concurrently and the field being deleted.
                Optional<String> resolvedFieldLatexFree = bibEntry.getResolvedFieldOrAliasLatexFree(field.getKey(), this.databaseContext.getDatabase());
                assert resolvedFieldLatexFree.isPresent();
                preparedStatement.setString(4, resolvedFieldLatexFree.orElse(""));

                preparedStatement.addBatch();
            }

            // add entry type
            preparedStatement.setString(1, entryId);
            preparedStatement.setString(2, SearchFieldConstants.ENTRY_TYPE.toString());
            preparedStatement.setString(3, bibEntry.getType().getName());
            preparedStatement.setString(4, bibEntry.getType().getName());
            preparedStatement.addBatch();

            preparedStatement.executeBatch();
        } catch (SQLException e) {
            LOGGER.error("Could not add an entry to the index.", e);
        }
    }

    public void removeFromIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
        task.setTitle(Localization.lang("Removing entries from index for %0", libraryName));
        int i = 1;
        for (BibEntry entry : entries) {
            if (task.isCancelled()) {
                LOGGER.debug("Removing entries canceled");
                return;
            }
            removeFromIndex(entry);
            task.updateProgress(i, entries.size());
            task.updateMessage(Localization.lang("%0 of %1 entries removed from the index.", i, entries.size()));
            i++;
        }
    }

    private void removeFromIndex(BibEntry entry) {
        try {
            connection.createStatement().executeUpdate("""
                    DELETE FROM "%s"
                    WHERE "%s" = '%s'
                    """.formatted(tableName, PostgreConstants.ENTRY_ID, entry.getId()));
            LOGGER.debug("Entry {} removed from index", entry.getId());
        } catch (SQLException e) {
            LOGGER.error("Error deleting entry from index", e);
        }
    }

    public void updateEntry(BibEntry entry, Field field) {
        try {
            // Use upsert to add the field to the index if it doesn't exist, or update it if it does
            String updateQuery = """
            INSERT INTO "%s" ("%s", "%s", "%s", "%s")
            VALUES (?, ?, ?, ?)
            ON CONFLICT ("%s", "%s") DO UPDATE
            SET "%s" = EXCLUDED."%s"
            """.formatted(tableName,
                    PostgreConstants.ENTRY_ID,
                    PostgreConstants.FIELD_NAME,
                    PostgreConstants.FIELD_VALUE_LITERAL,
                    PostgreConstants.FIELD_VALUE_TRANSFORMED,
                    PostgreConstants.ENTRY_ID,
                    PostgreConstants.FIELD_NAME,
                    PostgreConstants.FIELD_VALUE_LITERAL,
                    PostgreConstants.FIELD_VALUE_LITERAL);

            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setString(1, entry.getId());
                preparedStatement.setString(2, field.getName());
                preparedStatement.setString(3, entry.getField(field).orElse(""));
                preparedStatement.executeUpdate();
                LOGGER.debug("Updated entry {} in index", entry.getId());
            }
        } catch (SQLException e) {
            LOGGER.error("Error updating entry in index", e);
        }
    }

    public void close() {
        HeadlessExecutorService.INSTANCE.execute(this::closeIndex);
    }

    public void closeAndWait() {
        HeadlessExecutorService.INSTANCE.executeAndWait(this::closeIndex);
    }

    private void closeIndex() {
        try {
            LOGGER.debug("Closing connection to Postgres server for library: {}", libraryName);
            connection.createStatement().executeUpdate("""
                        DROP TABLE IF EXISTS "%s"
                        """.formatted(tableName));
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("Could not drop table for library: {}", libraryName, e);
        }
    }
}
