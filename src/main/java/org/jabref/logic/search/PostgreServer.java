package org.jabref.logic.search;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.jabref.logic.util.Directories;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgreConstants.BIB_FIELDS_SCHEME;
import static org.jabref.model.search.PostgreConstants.LINKED_FILES_SCHEME;

public class PostgreServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreServer.class);
    private final EmbeddedPostgres embeddedPostgres;
    private final DataSource dataSource;

    public PostgreServer() {
        EmbeddedPostgres embeddedPostgres;
        try {
            embeddedPostgres = EmbeddedPostgres.builder()
                                               .setCleanDataDirectory(false)
                                               .setDataDirectory(Directories.getPostgresDataDirectory())
                                               .setPort(10456)
                                               .start();
            LOGGER.info("Postgres server started, connection port: {}", embeddedPostgres.getPort());
        } catch (IOException e) {
            LOGGER.error("Could not start Postgres server", e);
            this.embeddedPostgres = null;
            this.dataSource = null;
            return;
        }

        this.embeddedPostgres = embeddedPostgres;
        this.dataSource = embeddedPostgres.getPostgresDatabase();
        addTrigramExtension();
        createScheme();
    }

    private void createScheme() {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                LOGGER.debug("Creating scheme for bib fields and linked files");
                connection.createStatement().execute("DROP SCHEMA IF EXISTS " + BIB_FIELDS_SCHEME);
                connection.createStatement().execute("CREATE SCHEMA " + BIB_FIELDS_SCHEME);
                connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS " + LINKED_FILES_SCHEME);
            }
        } catch (SQLException e) {
            LOGGER.error("Could not create scheme for bib fields and linked files", e);
        }
    }

    private void addTrigramExtension() {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                LOGGER.debug("Adding trigram extension to Postgres server");
                connection.createStatement().execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            }
        } catch (SQLException e) {
            LOGGER.error("Could not add trigram extension to Postgres server", e);
        }
    }

    public Connection getConnection() {
        if (dataSource != null) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                LOGGER.error("Could not get connection to Postgres server", e);
            }
        }
        return null;
    }

    public void shutdown() {
        if (embeddedPostgres != null) {
            try {
                embeddedPostgres.close();
            } catch (IOException e) {
                LOGGER.error("Could not shutdown Postgres server", e);
            }
        }
    }
}
