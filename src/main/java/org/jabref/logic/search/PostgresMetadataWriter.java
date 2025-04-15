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

public class PostgresMetadataWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresMetadataWriter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PostgresMetadataWriter() {
    }

    public static Path getMetadataFilePath() {
        return Path.of(System.getProperty("java.io.tmpdir"),
                       "jabref-postgres-info-" + ProcessHandle.current().pid() + ".json");
    }

    public static void write(int port) {
        try {
            Map<String, Object> meta = createMetadata(port);
            Path path = getMetadataFilePath();
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), meta);
            LOGGER.info("Postgres metadata file path: {}", path);
        } catch (IOException e) {
            LOGGER.warn("Failed to write Postgres metadata file", e);
        }
    }

    private static Map<String, Object> createMetadata(int port) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("javaPid", ProcessHandle.current().pid());
        meta.put("postgresPort", port);
        meta.put("startedBy", "jabref");
        meta.put("startedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        return meta;
    }
}
