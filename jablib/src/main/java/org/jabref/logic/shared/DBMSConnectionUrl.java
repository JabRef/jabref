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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A PostgreSQL connection URL as handed out by hosting providers or used with JDBC, e.g.
/// `postgres://user:secret@host:5432/db?sslmode=require` or `jdbc:postgresql://host/db?user=me`.
/// Surrounding text is ignored, so a whole `psql 'postgres://…'` command line can be pasted as well.
///
/// @param query the query part without `user`, `password`, `ssl`, and `sslmode=require`, which map to dedicated settings; empty if none
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
    private static final Set<String> SSL_MODES_VERIFYING_SERVER = Set.of("verify-ca", "verify-full");
    private static final Pattern URL_IN_TEXT = Pattern.compile("(?i)(?:jdbc:)?postgres(?:ql)?://[^\\s'\"]+");

    // [impl->req~shared-database.connection-url~1]
    public static Optional<DBMSConnectionUrl> parse(@Nullable String text) {
        if (StringUtil.isBlank(text)) {
            return Optional.empty();
        }
        Matcher matcher = URL_IN_TEXT.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String url = matcher.group();
        if (url.regionMatches(true, 0, "jdbc:", 0, 5)) {
            url = url.substring(5);
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        if (StringUtil.isBlank(uri.getHost())) {
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
                    String mode = value.toLowerCase(Locale.ROOT);
                    useSSL = SSL_MODES_REQUIRING_SSL.contains(mode);
                    // JabRef's "Use SSL" is sslmode=require; stricter modes are only expressible in the JDBC URL
                    if (SSL_MODES_VERIFYING_SERVER.contains(mode)) {
                        remaining.add(parameter);
                    }
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
