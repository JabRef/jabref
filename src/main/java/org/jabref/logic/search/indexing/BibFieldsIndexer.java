package org.jabref.logic.search.indexing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.PostgreConstants;
import org.jabref.model.search.SearchFieldConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgreConstants.ENTRY_ID;
import static org.jabref.model.search.PostgreConstants.FIELD_NAME;
import static org.jabref.model.search.PostgreConstants.FIELD_VALUE_LITERAL;
import static org.jabref.model.search.PostgreConstants.FIELD_VALUE_TRANSFORMED;
import static org.jabref.model.search.PostgreConstants.TABLE_NAME_SUFFIX;

public class BibFieldsIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibFieldsIndexer.class);
    private static final LatexToUnicodeFormatter LATEX_TO_UNICODE_FORMATTER = new LatexToUnicodeFormatter();
    private static final Pattern GROUPS_SEPARATOR_REGEX = Pattern.compile("\s*,\s*");

    private final BibDatabaseContext databaseContext;
    private final Connection connection;
    private final String libraryName;
    private final String mainTable;
    private final String schemaMainTableReference;
    private final String splitValuesTable;
    private final String schemaSplitValuesTableReference;
    private final Character keywordSeparator;

    public BibFieldsIndexer(BibEntryPreferences bibEntryPreferences, BibDatabaseContext databaseContext, Connection connection) {
        this.databaseContext = databaseContext;
        this.connection = connection;
        this.keywordSeparator = bibEntryPreferences.getKeywordSeparator();
        this.libraryName = databaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElse("unsaved");

        this.mainTable = databaseContext.getUniqueName();
        this.splitValuesTable = mainTable + TABLE_NAME_SUFFIX;

        this.schemaMainTableReference = """
                "%s"."%s"
                """.formatted(PostgreConstants.BIB_FIELDS_SCHEME, mainTable);
        this.schemaSplitValuesTableReference = """
                "%s"."%s"
                """.formatted(PostgreConstants.BIB_FIELDS_SCHEME, splitValuesTable);
        // TODO: Set-up should be in a background task
        setup();
    }

    public String getMainTable() {
        return mainTable;
    }

    /**
     * Creates a table for the library in the database, and sets up indexes on the columns.
     */
    private void setup() {
        try {
            connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                        %s TEXT NOT NULL,
                        %s TEXT NOT NULL,
                        %s TEXT,
                        %s TEXT,
                        PRIMARY KEY (%s, %s)
                    )
                    """.formatted(
                    schemaMainTableReference,
                    ENTRY_ID,
                    FIELD_NAME,
                    FIELD_VALUE_LITERAL,
                    FIELD_VALUE_TRANSFORMED,
                    ENTRY_ID, FIELD_NAME));

            connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                        %s TEXT NOT NULL,
                        %s TEXT NOT NULL,
                        %s TEXT,
                        %s TEXT
                        )
                    """.formatted(
                    schemaSplitValuesTableReference,
                    ENTRY_ID,
                    FIELD_NAME,
                    FIELD_VALUE_LITERAL,
                    FIELD_VALUE_TRANSFORMED));

            LOGGER.debug("Created tables for library: {}", libraryName);
        } catch (SQLException e) {
            LOGGER.error("Could not create tables for library: {}", libraryName, e);
        }
        try {
            // region btree index on id column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s_%s_index" ON %s ("%s")
                    """.formatted(
                    mainTable, ENTRY_ID,
                    schemaMainTableReference,
                    ENTRY_ID));

            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s_%s_index" ON %s ("%s")
                    """.formatted(
                    splitValuesTable, ENTRY_ID,
                    schemaSplitValuesTableReference,
                    ENTRY_ID));
            // endregion

            // region btree index on field name column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s_%s_index" ON %s ("%s")
                    """.formatted(
                    mainTable, FIELD_NAME,
                    schemaMainTableReference,
                    FIELD_NAME));

            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s_%s_index" ON %s ("%s")
                    """.formatted(
                    splitValuesTable, FIELD_NAME,
                    schemaSplitValuesTableReference,
                    FIELD_NAME));
            // endregion

            // trigram index on field value column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s_%s_index" ON %s USING gin ("%s" gin_trgm_ops)
                    """.formatted(
                    mainTable, FIELD_VALUE_LITERAL,
                    schemaMainTableReference,
                    FIELD_VALUE_LITERAL));

            // trigram index on field value transformed column
            connection.createStatement().executeUpdate("""
                    CREATE INDEX "%s_%s_index" ON %s USING gin ("%s" gin_trgm_ops)
                    """.formatted(
                    mainTable, FIELD_VALUE_TRANSFORMED,
                    schemaMainTableReference,
                    FIELD_VALUE_TRANSFORMED));

            // region btree index on spilt values column
//            connection.createStatement().executeUpdate("""
//                    CREATE INDEX "%s" ON "%s" ("%s")
//                    """.formatted(
//                    FIELD_VALUE_LITERAL.getIndexName(tableNameSplitValues),
//                    tableName,
//                    FIELD_VALUE_LITERAL));
//
//            connection.createStatement().executeUpdate("""
//                    CREATE INDEX "%s" ON "%s" ("%s")
//                    """.formatted(
//                    FIELD_VALUE_TRANSFORMED.getIndexName(tableNameSplitValues),
//                    tableName,
//                    FIELD_VALUE_TRANSFORMED));
            // endregion

            LOGGER.debug("Created indexes for library: {}", libraryName);
        } catch (SQLException e) {
            LOGGER.error("Could not create indexes for library: {}", libraryName, e);
        }
    }

    public void updateOnStart(BackgroundTask<?> task) {
        addToIndex(databaseContext.getDatabase().getEntries(), task);
    }

    public void addToIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
        if (entries.size() > 1) {
            task.showToUser(true);
            task.setTitle(Localization.lang("Indexing bib fields for %0", libraryName));
        }
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
                INSERT INTO %s ("%s", "%s", "%s", "%s")
                VALUES (?, ?, ?, ?)
                """.formatted(
                schemaMainTableReference,
                ENTRY_ID,
                FIELD_NAME,
                FIELD_VALUE_LITERAL,
                FIELD_VALUE_TRANSFORMED);

        String insertIntoSplitTable = """
                INSERT INTO %s ("%s", "%s", "%s", "%s")
                VALUES (?, ?, ?, ?)
                """.formatted(
                schemaSplitValuesTableReference,
                ENTRY_ID,
                FIELD_NAME,
                FIELD_VALUE_LITERAL,
                FIELD_VALUE_TRANSFORMED);

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertFieldQuery);
             PreparedStatement preparedStatementSplitValues = connection.prepareStatement(insertIntoSplitTable)) {
            String entryId = bibEntry.getId();
            for (Map.Entry<Field, String> fieldPair : bibEntry.getFieldMap().entrySet()) {
                Field field = fieldPair.getKey();
                String value = fieldPair.getValue();

                // If a field exists, there also exists a resolved field latex free.
                // We add a `.orElse("")` only because there could be some flaw in the future in the code - and we want to have search working even if the flaws are present.
                // To uncover these flaws, we add the "assert" statement.
                // One potential future flaw is that the bibEntry is modified concurrently and the field being deleted.
                Optional<String> resolvedFieldLatexFree = bibEntry.getResolvedFieldOrAliasLatexFree(field, this.databaseContext.getDatabase());
                assert resolvedFieldLatexFree.isPresent();
                addBatch(preparedStatement, entryId, field, value, resolvedFieldLatexFree.orElse(""));

                // region Handling of known multi-value fields
                // split and convert to Unicode
                if (field.getProperties().contains(FieldProperty.PERSON_NAMES)) {
                    addAuthors(value, preparedStatementSplitValues, entryId, field);
                } else if (field == StandardField.KEYWORDS) {
                    addKeywords(value, preparedStatementSplitValues, entryId, field, keywordSeparator);
                } else if (field == StandardField.GROUPS) {
                    addGroups(value, preparedStatementSplitValues, entryId, field);
                } else if (field.getProperties().contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                    addEntryLinks(bibEntry, field, preparedStatementSplitValues, entryId);
                } else if (field == StandardField.FILE) {
                    // No handling of File, because due to relative paths, we think, there won't be any exact match operation
                    // We could add the filename itself (with and without extension). However, the user can also use regular expressions to achieve the same.
                    // The use case to search for file names seems pretty seldom, therefore we omit it.
                } else {
                    // No other multi-value fields are known
                    // No action needed -> main table has the value
                }
                // endregion
            }

            // add entry type
            // Separate code, because ENTRY_TYPE is not a Field
            preparedStatement.setString(1, entryId);
            preparedStatement.setString(2, SearchFieldConstants.ENTRY_TYPE.toString());
            preparedStatement.setString(3, bibEntry.getType().getName());
            preparedStatement.setString(4, bibEntry.getType().getName());
            preparedStatement.addBatch();

            preparedStatement.executeBatch();
            preparedStatementSplitValues.executeBatch();
        } catch (SQLException e) {
            LOGGER.error("Could not add an entry to the index.", e);
        }
    }

    private void addEntryLinks(BibEntry bibEntry, Field field, PreparedStatement preparedStatementSplitValues, String entryId) {
        bibEntry.getEntryLinkList(field, databaseContext.getDatabase()).stream().distinct().forEach(link -> {
            addBatch(preparedStatementSplitValues, entryId, field, link.getKey());
        });
    }

    private static void addGroups(String value, PreparedStatement preparedStatementSplitValues, String entryId, Field field) {
        // We could use KeywordList, but we are afraid that group names could have ">" in their name, and then they would not be handled correctly
        Arrays.stream(GROUPS_SEPARATOR_REGEX.split(value))
              .distinct()
              .forEach(group -> {
                  addBatch(preparedStatementSplitValues, entryId, field, group);
              });
    }

    private static void addKeywords(String keywordsString, PreparedStatement preparedStatementSplitValues, String entryId, Field field, Character keywordSeparator) {
        KeywordList keywordList = KeywordList.parse(keywordsString, keywordSeparator);
        keywordList.stream().flatMap(keyword -> keyword.flatten().stream()).forEach(keyword -> {
            String value = keyword.toString();
            addBatch(preparedStatementSplitValues, entryId, field, value);
        });
    }

    private static void addAuthors(String value, PreparedStatement preparedStatementSplitValues, String entryId, Field field) {
        AuthorList.parse(value).getAuthors().forEach(author -> {
            // Author object does not support literal values
            // We use the method giving us the most complete information for the literal value;
            String literal = author.getGivenFamily(false);
            String transformed = author.latexFree().getGivenFamily(false);
            addBatch(preparedStatementSplitValues, entryId, field, literal, transformed);
        });
    }

    private static void addBatch(PreparedStatement preparedStatement, String entryId, Field field, String value) {
        addBatch(preparedStatement, entryId, field, value, LATEX_TO_UNICODE_FORMATTER.format(value));
    }

    /**
     * The values are passed as they should be inserted into the database table
     */
    private static void addBatch(PreparedStatement preparedStatement, String entryId, Field field, String value, String normalized) {
        try {
            preparedStatement.setString(1, entryId);
            preparedStatement.setString(2, field.getName());
            preparedStatement.setString(3, value);
            preparedStatement.setString(4, normalized);
            preparedStatement.addBatch();
        } catch (SQLException e) {
            LOGGER.error("Could not add field {} having value {} of entry {} to the index.", field.getName(), value, entryId, e);
        }
    }

    public void removeFromIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
        if (entries.size() > 1) {
            task.showToUser(true);
            task.setTitle(Localization.lang("Removing entries from index for %0", libraryName));
        }
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
                    DELETE FROM %s
                    WHERE "%s" = '%s'
                    """.formatted(schemaMainTableReference, ENTRY_ID, entry.getId()));
            LOGGER.debug("Entry {} removed from index", entry.getId());
        } catch (SQLException e) {
            LOGGER.error("Error deleting entry from index", e);
        }
    }

    public void updateEntry(BibEntry entry, Field field, String oldValue, String newValue) {
        try {
            String updateQuery = """
            INSERT INTO %s ("%s", "%s", "%s", "%s")
            VALUES (?, ?, ?, ?)
            """.formatted(schemaSplitValuesTableReference,
                    ENTRY_ID,
                    FIELD_NAME,
                    FIELD_VALUE_LITERAL,
                    FIELD_VALUE_TRANSFORMED);

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
                        DROP TABLE IF EXISTS %s
                        """.formatted(schemaMainTableReference));
            connection.createStatement().executeUpdate("""
                        DROP TABLE IF EXISTS %s
                        """.formatted(schemaSplitValuesTableReference));
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("Could not drop table for library: {}", libraryName, e);
        }
    }
}
