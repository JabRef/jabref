package org.jabref.logic.shared;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A PostgreSQL connection URL as handed out by hosting providers or used with JDBC, e.g.
/// `postgres://user:secret@host:5432/db?sslmode=require` or `jdbc:postgresql://host/db?user=me`.
///
/// @param query the query part without `user`, `password`, and `ssl`, which map to dedicated settings; empty if none
@NullMarked
public record DBMSConnectionUrl(DBMSType type,
                                String host,
                                int port,
                                String database,
                                Optional<String> user,
                                Optional<String> password,
                                boolean useSSL,
                                String query) {

    private static final Set<String> SSL_MODES_REQUIRING_SSL = Set.of("require", "verify-ca", "verify-full");

    // [impl->req~shared-database.connection-url~1]
    public static Optional<DBMSConnectionUrl> parse(@Nullable String text) {
        if (StringUtil.isBlank(text)) {
            return Optional.empty();
        }
        String trimmed = text.strip();
        if (trimmed.regionMatches(true, 0, "jdbc:", 0, 5)) {
            trimmed = trimmed.substring(5);
        }
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme();
        boolean isPostgres = "postgres".equalsIgnoreCase(scheme) || "postgresql".equalsIgnoreCase(scheme);
        if (!isPostgres || StringUtil.isBlank(uri.getHost())) {
            return Optional.empty();
        }

        DBMSType type = DBMSType.POSTGRESQL;
        int port = uri.getPort() == -1 ? type.getDefaultPort() : uri.getPort();
        String path = Optional.ofNullable(uri.getPath()).orElse("");
        String database = path.startsWith("/") ? path.substring(1) : path;

        Optional<String> user = Optional.empty();
        Optional<String> password = Optional.empty();
        String rawUserInfo = Optional.ofNullable(uri.getRawUserInfo()).orElse("");
        if (!rawUserInfo.isEmpty()) {
            int colon = rawUserInfo.indexOf(':');
            user = Optional.of(decode(colon < 0 ? rawUserInfo : rawUserInfo.substring(0, colon)));
            if (colon >= 0) {
                password = Optional.of(decode(rawUserInfo.substring(colon + 1)));
            }
        }

        boolean useSSL = false;
        List<String> remaining = new ArrayList<>();
        String rawQuery = Optional.ofNullable(uri.getRawQuery()).orElse("");
        if (!rawQuery.isEmpty()) {
            for (String parameter : rawQuery.split("&")) {
                int equals = parameter.indexOf('=');
                String key = decode(equals < 0 ? parameter : parameter.substring(0, equals));
                String value = equals < 0 ? "" : decode(parameter.substring(equals + 1));
                if ("user".equals(key)) {
                    user = Optional.of(user.orElse(value));
                } else if ("password".equals(key)) {
                    password = Optional.of(password.orElse(value));
                } else if ("ssl".equals(key)) {
                    useSSL = value.isEmpty() || Boolean.parseBoolean(value);
                } else if ("sslmode".equals(key)) {
                    useSSL = SSL_MODES_REQUIRING_SSL.contains(value.toLowerCase(Locale.ROOT));
                    // Kept: JabRef's "Use SSL" means verify-full, which is stricter than e.g. sslmode=require
                    remaining.add(parameter);
                } else {
                    remaining.add(parameter);
                }
            }
        }

        return Optional.of(new DBMSConnectionUrl(type, uri.getHost(), port, database, user, password, useSSL, String.join("&", remaining)));
    }

    /// The URL for the JDBC driver, keeping every parameter JabRef has no dedicated setting for.
    public String toJdbcUrl() {
        String url = type.getUrl(host, port, database);
        return query.isEmpty() ? url : url + "?" + query;
    }

    private static String decode(String raw) {
        // URLDecoder is made for HTML forms: it would turn a literal '+' into a space
        return URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
