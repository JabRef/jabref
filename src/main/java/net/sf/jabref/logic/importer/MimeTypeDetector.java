/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import com.mashape.unirest.http.Unirest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MimeTypeDetector {
    private static final Log LOGGER = LogFactory.getLog(MimeTypeDetector.class);

    public static boolean isPdfContentType(String url) {
        Optional<String> contentType = getMimeType(url);

        return contentType.isPresent() && contentType.get().toLowerCase().startsWith("application/pdf");
    }

    private static Optional<String> getMimeType(String url) {
        Unirest.setDefaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

        // Try to use HEAD request to avoid donloading the whole file
        String contentType;
        try {
            contentType = Unirest.head(url).asString().getHeaders().get("Content-Type").get(0);

            if (contentType != null) {
                return Optional.of(contentType);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via HEAD request", e);
        }

        // Use GET request as alternative if no HEAD request is available
        try {
            contentType = Unirest.get(url).asString().getHeaders().get("Content-Type").get(0);

            if (contentType != null) {
                return Optional.of(contentType);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via GET request", e);
        }

        // Try to resolve local URIs
        try {
            URLConnection connection = new URL(url).openConnection();

            return Optional.ofNullable(connection.getContentType());
        } catch (IOException e) {
            LOGGER.debug("Error trying to get MIME type of local URI", e);
        }

        return Optional.empty();
    }
}
