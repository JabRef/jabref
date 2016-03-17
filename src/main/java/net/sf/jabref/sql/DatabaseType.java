package net.sf.jabref.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * All DBTypes must appear here. The enum items must be the
 * names that appear in the combobox used to select the DB,
 * because this text is used to choose which DatabaseImporter/Exporter
 * will be sent back to the requester
 */
public enum DatabaseType {
    MYSQL("MySQL"), POSTGRESQL("PostgreSQL");

    private final String serverType;

    DatabaseType(String dbType) {
        this.serverType = dbType;
    }

    public String getServerType() {
        return serverType;
    }

    public static Optional<DatabaseType> build(String serverType) {
        for(DatabaseType type : values()) {
            if(type.getServerType().equalsIgnoreCase(serverType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static List<String> SERVER_TYPES = Arrays
            .stream(DatabaseType.values())
            .map(DatabaseType::getServerType)
            .collect(Collectors.toList());
}
