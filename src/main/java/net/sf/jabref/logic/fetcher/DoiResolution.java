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
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that follows the DOI resolution redirects and scans for a full-text PDF URL.
 */
public class DoiResolution implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(DoiResolution.class);

    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Follow all redirects and scan for a single pdf link
        Optional<DOI> doi = DOI.build(entry.getField("doi"));

        if(doi.isPresent()) {
            // Available in catalog?
            try {
                String sciLink = getUrlByDoi(doi.get().getDOI());

                if (!sciLink.isEmpty()) {
                    // Retrieve PDF link
                    Document html = Jsoup.connect(sciLink).ignoreHttpErrors(true).get();
                    Element link = html.getElementById("pdfLink");

                    if (link != null) {
                        LOGGER.info("Fulltext PDF found @ ScienceDirect.");
                        pdfLink = Optional.of(new URL(link.attr("pdfurl")));
                    }
                }
            } catch(UnirestException e) {
                LOGGER.warn("ScienceDirect API request failed: " + e.getMessage(), e);
            }
        }

        // TODO: title search
        // We can also get abstract automatically!
        return pdfLink;
    }
}
