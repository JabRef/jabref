package org.jabref.logic.shared;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A PostgreSQL connection URL as handed out by hosting providers or used with JDBC, e.g.
/// `postgres://user:secret@host:5432/db?sslmode=require` or `jdbc:postgresql://host/db?user=me`.
/// Surrounding text is ignored, so a whole `psql 'postgres://…'` command line can be pasted as well, and so can
/// libpq's keyword form (`host=… port=… dbname=… user=… password=…`).
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
    private static final Pattern KEYWORD_VALUE = Pattern.compile("(\\w+)\\s*=\\s*('(?:\\\\.|[^'])*'|\\S+)");

    // [impl->req~shared-database.connection-url~1]
    public static Optional<DBMSConnectionUrl> parse(@Nullable String text) {
        if (StringUtil.isBlank(text)) {
            return Optional.empty();
        }
        Matcher urlMatcher = URL_IN_TEXT.matcher(text);
        if (urlMatcher.find()) {
            return parseUrl(urlMatcher.group());
        }
        return parseKeywords(text);
    }

    private static Optional<DBMSConnectionUrl> parseUrl(String url) {
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

        String path = Optional.ofNullable(uri.getPath()).orElse("");
        List<Map.Entry<String, String>> parameters = new ArrayList<>();
        String rawUserInfo = Optional.ofNullable(uri.getRawUserInfo()).orElse("");
        if (!rawUserInfo.isEmpty()) {
            int colon = rawUserInfo.indexOf(':');
            parameters.add(Map.entry("user", decode(colon < 0 ? rawUserInfo : rawUserInfo.substring(0, colon))));
            if (colon >= 0) {
                parameters.add(Map.entry("password", decode(rawUserInfo.substring(colon + 1))));
            }
        }
        String rawQuery = Optional.ofNullable(uri.getRawQuery()).orElse("");
        if (!rawQuery.isEmpty()) {
            for (String parameter : rawQuery.split("&")) {
                int equals = parameter.indexOf('=');
                String key = decode(equals < 0 ? parameter : parameter.substring(0, equals));
                String value = equals < 0 ? "" : decode(parameter.substring(equals + 1));
                parameters.add(Map.entry(key, value));
            }
        }
        return Optional.of(fromParts(uri.getHost(), Optional.of(uri.getPort()).filter(port -> port != -1), path.startsWith("/") ? path.substring(1) : path, parameters));
    }

    /// libpq keyword form as shown by some providers: `host=db.example.org port=5432 dbname=lib user=me password='p w'`
    private static Optional<DBMSConnectionUrl> parseKeywords(String text) {
        Matcher matcher = KEYWORD_VALUE.matcher(text);
        String host = "";
        Optional<Integer> port = Optional.empty();
        String database = "";
        List<Map.Entry<String, String>> parameters = new ArrayList<>();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = unquote(matcher.group(2));
            if ("host".equals(key) || "hostaddr".equals(key)) {
                host = value;
            } else if ("port".equals(key)) {
                port = parsePort(value);
            } else if ("dbname".equals(key)) {
                database = value;
            } else {
                parameters.add(Map.entry(key, value));
            }
        }
        if (StringUtil.isBlank(host)) {
            return Optional.empty();
        }
        return Optional.of(fromParts(host, port, database, parameters));
    }

    private static DBMSConnectionUrl fromParts(String host, Optional<Integer> port, String database, List<Map.Entry<String, String>> parameters) {
        DBMSType type = DBMSType.POSTGRESQL;
        Optional<String> user = Optional.empty();
        Optional<String> password = Optional.empty();
        boolean useSSL = false;
        List<String> remaining = new ArrayList<>();
        for (Map.Entry<String, String> parameter : parameters) {
            String key = parameter.getKey();
            String value = parameter.getValue();
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
                    remaining.add(encode(key) + "=" + encode(value));
                }
            } else {
                remaining.add(encode(key) + "=" + encode(value));
            }
        }
        return new DBMSConnectionUrl(type, host, port.orElse(type.getDefaultPort()), database, user, password, useSSL, String.join("&", remaining));
    }

    private static Optional<Integer> parsePort(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /// Strips libpq's single quotes and their backslash escapes
    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1).replace("\\'", "'").replace("\\\\", "\\");
        }
        return value;
    }

    /// The URL for the JDBC driver, keeping every parameter JabRef has no dedicated setting for.
    public String toJdbcUrl() {
        String url = type.getUrl(host, port, database);
        return query.isEmpty() ? url : url + "?" + query;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String decode(String raw) {
        // URLDecoder is made for HTML forms: it would turn a literal '+' into a space
        return URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
