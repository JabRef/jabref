/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
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
            String name = nameValue.substring(0, equals);
            String value = nameValue.substring(equals + 1);
            if ("domain".equalsIgnoreCase(name)) {
                String uriDomain = uri.getHost();
                if (uriDomain.equals(value)) {
                    this.domain = value;
                } else {
                    if (!value.startsWith(".")) {
                        value = '.' + value;
                    }
                    uriDomain = uriDomain.substring(uriDomain.indexOf('.'));
                    if (!uriDomain.equals(value) && !uriDomain.endsWith(value)
                            && !value.endsWith(uriDomain)) {
                        throw new IllegalArgumentException("Trying to set foreign cookie");
                    }
                    this.domain = value;
                }
            } else if ("path".equalsIgnoreCase(name)) {
                this.path = value;
            } else if ("expires".equalsIgnoreCase(name)) {
                try {
                    this.expires = ZonedDateTime.parse(value, whiteSpaceFormat);
                } catch (DateTimeParseException e) {
                    try {
                        this.expires = ZonedDateTime.parse(value, hyphenFormat);
                    } catch (DateTimeParseException e2) {
                        try {
                            this.expires = ZonedDateTime.parse(value, hyphenTwoDigitYearFormat);
                        } catch (DateTimeParseException e3) {
                            throw new IllegalArgumentException("Bad date format in header: " + value);
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
