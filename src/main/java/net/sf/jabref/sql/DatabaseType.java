package net.sf.jabref.sql;

import java.util.Optional;

/**
 * All DBTypes must appear here. The enum items must be the
 * names that appear in the combobox used to select the DB,
 * because this text is used to choose which DatabaseImporter/Exporter
 * will be sent back to the requester
 */
public enum DatabaseType {
    MYSQL("MySQL"), POSTGRESQL("PostgreSQL");

    private final String formattedName;

    DatabaseType(String formattedName) {
        this.formattedName = formattedName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public static Optional<DatabaseType> build(String serverType) {
        for (DatabaseType type : values()) {
            if (type.getFormattedName().equalsIgnoreCase(serverType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return formattedName;
    }

}
