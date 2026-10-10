package org.jabref.logic.shared;

import java.util.Locale;

import org.jspecify.annotations.NullMarked;

// [impl->req~shared-database.single-tab~1]
@NullMarked
public record SharedDatabaseIdentity(DBMSType type, String host, int port, String database) {

    public static SharedDatabaseIdentity from(DatabaseConnectionProperties properties) {
        if (properties.isUseExpertMode()) {
            return DBMSConnectionUrl.parse(properties.getJdbcUrl())
                                     .map(url -> new SharedDatabaseIdentity(url.type(), normalizeHost(url.host()), url.port(), url.database()))
                                     .orElseGet(() -> fromFields(properties));
        }
        return fromFields(properties);
    }

    private static SharedDatabaseIdentity fromFields(DatabaseConnectionProperties properties) {
        return new SharedDatabaseIdentity(properties.getType(), normalizeHost(properties.getHost()), properties.getPort(), properties.getDatabase());
    }

    private static String normalizeHost(String host) {
        return host.toLowerCase(Locale.ROOT);
    }
}
