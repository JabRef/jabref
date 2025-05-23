package org.jabref.logic.search;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.util.Directories.getTmpDirectory;

public class PostgresMetadataWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresMetadataWriter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String JAVA_PID = "javaPid";
    private static final String POSTGRES_PORT = "postgresPort";
    private static final String STARTED_BY = "startedBy";
    private static final String STARTED_AT = "startedAt";
    private static final String JABREF = "jabref";

    private PostgresMetadataWriter() {
    }

    public static Path getMetadataFilePath() {
        return getTmpDirectory().resolve("jabref-postgres-info-" + ProcessHandle.current().pid() + ".json");
    }

    public static void write(int port) {
        try {
            Path path = getMetadataFilePath();
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), createMetadata(port));
            LOGGER.info("Postgres metadata file path: {}", path);
        } catch (IOException e) {
            LOGGER.warn("Failed to write Postgres metadata file.", e);
        }
    }

    private static Map<String, Object> createMetadata(int port) {
        Map<String, Object> meta = new HashMap<>();
        meta.put(JAVA_PID, ProcessHandle.current().pid());
        meta.put(POSTGRES_PORT, port);
        meta.put(STARTED_BY, JABREF);
        meta.put(STARTED_AT, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        return meta;
    }
}
