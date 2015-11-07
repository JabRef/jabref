/*  Copyright (C) 2015 JabRef contributors.
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
package net.sf.jabref.logic.fetcher;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.DOI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.net.URL;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL at ScienceDirect.
 *
 * @see http://dev.elsevier.com/
 */
public class ScienceDirect implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(ScienceDirect.class);

    private static final String API_URL = "http://api.elsevier.com/content/article/doi/";
    private static final String API_KEY = "fb82f2e692b3c72dafe5f4f1fa0ac00b";
    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Try unique DOI first
        Optional<DOI> doi = DOI.build(entry.getField("doi"));

        if(doi.isPresent()) {
            // Available in catalog?
            try {
                String request = API_URL + doi.get().getDOI();
                HttpResponse<InputStream> response = Unirest.get(request)
                        .header("X-ELS-APIKey", API_KEY)
                        .queryString("httpAccept", "application/pdf")
                        .asBinary();

                if (response.getStatus() == 200) {
                    LOGGER.info("Fulltext PDF found @ ScienceDirect.");
                    pdfLink = Optional.of(new URL(request + "?httpAccept=application/pdf"));
                }
            } catch(UnirestException e) {
                LOGGER.warn("Elsevier API request failed: " + e.getMessage(), e);
            }
        }

        // TODO: title search
        // We can also get abstract automatically!
        return pdfLink;
    }
}
