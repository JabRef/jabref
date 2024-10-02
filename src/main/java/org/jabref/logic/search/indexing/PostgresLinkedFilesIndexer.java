package org.jabref.logic.search.indexing;

import java.sql.Connection;
import java.sql.SQLException;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgreConstants.CONTENT_TABLE_SUFFIX;
import static org.jabref.model.search.PostgreConstants.FILE_MODIFIED;
import static org.jabref.model.search.PostgreConstants.FILE_PATH;
import static org.jabref.model.search.PostgreConstants.LINKED_ENTRIES;
import static org.jabref.model.search.PostgreConstants.LINKED_FILES_SCHEME;
import static org.jabref.model.search.PostgreConstants.PAGE_ANNOTATIONS;
import static org.jabref.model.search.PostgreConstants.PAGE_CONTENT;
import static org.jabref.model.search.PostgreConstants.PAGE_NUMBER;

public class PostgresLinkedFilesIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresLinkedFilesIndexer.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final Connection connection;
    private final String libraryName;
    private final String mainTable;
    private final String contentTable;
    private final String schemaMainTableReference;
    private final String schemaContentTableReference;

    public PostgresLinkedFilesIndexer(BibDatabaseContext databaseContext, FilePreferences filePreferences, Connection connection) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        this.connection = connection;
        this.libraryName = databaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElse("unsaved");
        this.mainTable = databaseContext.getUniqueName();
        this.contentTable = mainTable + CONTENT_TABLE_SUFFIX;
        this.schemaMainTableReference = """
                "%s"."%s"
                """.formatted(LINKED_FILES_SCHEME, mainTable);
        this.schemaContentTableReference = """
                "%s"."%s"
                """.formatted(LINKED_FILES_SCHEME, contentTable);
        setup();
    }

    private void setup() {
        try {
            connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                        %s TEXT NOT NULL,
                        %s TEXT NOT NULL,
                        %s TEXT NOT NULL,
                        PRIMARY KEY (%s)
                    )
                    """.formatted(
                    schemaMainTableReference,
                    FILE_PATH,
                    FILE_MODIFIED,
                    LINKED_ENTRIES,
                    FILE_PATH));

            connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                        %s TEXT NOT NULL REFERENCES %s(%s),
                        %s INT NOT NULL,
                        %s TEXT,
                        %s TEXT,
                        PRIMARY KEY (%s, %s)
                    )
                    """.formatted(
                    schemaContentTableReference,
                    FILE_PATH,
                    schemaMainTableReference, FILE_PATH,
                    PAGE_NUMBER,
                    PAGE_CONTENT,
                    PAGE_ANNOTATIONS,
                    FILE_PATH, PAGE_NUMBER));

            // full text index on page content, annotations
            connection.createStatement().executeUpdate("""
                    CREATE INDEX IF NOT EXISTS "%s_content_idx"
                    ON %s
                    USING GIN (to_tsvector('english', %s || ' ' || %s))
                    """.formatted(
                    contentTable,
                    schemaContentTableReference,
                    PAGE_CONTENT, PAGE_ANNOTATIONS));

            LOGGER.debug("Created full-text tables for library: {}", libraryName);
        } catch (SQLException e) {
            LOGGER.error("Could not create full-text tables for library: {}", libraryName, e);
        }
    }
}
