package org.jabref.logic.shared;

import java.util.Locale;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    /// Remembered connection settings may lack the host; that must not make two identities incomparable.
    private static String normalizeHost(@Nullable String host) {
        return (host == null) ? "" : host.toLowerCase(Locale.ROOT);
    }
}
