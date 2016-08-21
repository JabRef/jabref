package net.sf.jabref.logic.net;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

class Cookie {

    private final String name;
    private final String value;
    private String domain;
    private ZonedDateTime expires;
    private String path;

    private final DateTimeFormatter whiteSpaceFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z",
            Locale.ROOT);
    private final DateTimeFormatter hyphenFormat = DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm:ss z",
            Locale.ROOT);
    private final DateTimeFormatter hyphenTwoDigitYearFormat = DateTimeFormatter.ofPattern("EEE, dd-MMM-yy HH:mm:ss z",
            Locale.ROOT);


    /**
     * Construct a cookie from the URI and header fields
     *
     * @param uri URI for cookie
     * @param header Set of attributes in header
     */
    public Cookie(URI uri, String header) {
        String[] attributes = header.split(";");
        String nameValue = attributes[0].trim();
        this.name = nameValue.substring(0, nameValue.indexOf('='));
        this.value = nameValue.substring(nameValue.indexOf('=') + 1);
        this.path = "/";
        this.domain = uri.getHost();

        for (int i = 1; i < attributes.length; i++) {
            nameValue = attributes[i].trim();
            int equals = nameValue.indexOf('=');
            if (equals == -1) {
                continue;
            }
            String attributeName = nameValue.substring(0, equals);
            String attributeValue = nameValue.substring(equals + 1);
            if ("domain".equalsIgnoreCase(attributeName)) {
                String uriDomain = uri.getHost();
                if (uriDomain.equals(attributeValue)) {
                    this.domain = attributeValue;
                } else {
                    if (!attributeValue.startsWith(".")) {
                        attributeValue = '.' + attributeValue;
                    }
                    uriDomain = uriDomain.substring(uriDomain.indexOf('.'));
                    if (!uriDomain.equals(attributeValue) && !uriDomain.endsWith(attributeValue)
                            && !attributeValue.endsWith(uriDomain)) {
                        throw new IllegalArgumentException("Trying to set foreign cookie");
                    }
                    this.domain = attributeValue;
                }
            } else if ("path".equalsIgnoreCase(attributeName)) {
                this.path = attributeValue;
            } else if ("expires".equalsIgnoreCase(attributeName)) {
                try {
                    this.expires = ZonedDateTime.parse(attributeValue, whiteSpaceFormat);
                } catch (DateTimeParseException e) {
                    try {
                        this.expires = ZonedDateTime.parse(attributeValue, hyphenFormat);
                    } catch (DateTimeParseException e2) {
                        try {
                            this.expires = ZonedDateTime.parse(attributeValue, hyphenTwoDigitYearFormat);
                        } catch (DateTimeParseException e3) {
                            throw new IllegalArgumentException("Bad date format in header: " + attributeValue);
                        }
                    }
                }
            }
        }
    }

    public boolean hasExpired() {
        if (expires == null) {
            return false;
        }
        return ZonedDateTime.now().isAfter(expires);
    }

    /**
     * Check if cookie isn't expired and if URI matches,
     * should cookie be included in response.
     *
     * @param uri URI to check against
     * @return true if match, false otherwise
     */
    public boolean matches(URI uri) {

        if (hasExpired()) {
            return false;
        }

        String uriPath = Optional.ofNullable(uri.getPath()).orElse("/");

        return uriPath.startsWith(this.path);
    }

    public boolean equalNameAndDomain(Cookie cookie) {
        return ((domain.equals(cookie.domain)) && (name.equals(cookie.name)));
    }

    @Override
    public String toString() {
        return name + '=' + value;
    }
}
