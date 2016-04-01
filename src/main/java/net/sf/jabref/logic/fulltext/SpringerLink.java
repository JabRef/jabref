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
package net.sf.jabref.logic.fulltext;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.util.DOI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.net.URL;
import java.io.*;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL at SpringerLink.
 *
 * Uses Springer API, see @link{https://dev.springer.com}
 */
public class SpringerLink implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(SpringerLink.class);

    private static final String API_URL = "http://api.springer.com/meta/v1/json";
    private static final String API_KEY = "b0c7151179b3d9c1119cf325bca8460d";
    private static final String CONTENT_HOST = "link.springer.com";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Try unique DOI first
        Optional<DOI> doi = DOI.build(entry.getField("doi"));

        if(doi.isPresent()) {
            // Available in catalog?
            try {
                HttpResponse<JsonNode> jsonResponse = Unirest.get(API_URL)
                        .queryString("api_key", API_KEY)
                        .queryString("q", String.format("doi:%s", doi.get().getDOI()))
                        .asJson();

                JSONObject json = jsonResponse.getBody().getObject();
                int results = json.getJSONArray("result").getJSONObject(0).getInt("total");

                if (results > 0) {
                    LOGGER.info("Fulltext PDF found @ Springer.");
                    pdfLink = Optional.of(new URL("http", CONTENT_HOST, String.format("/content/pdf/%s.pdf", doi.get().getDOI())));
                }
            } catch(UnirestException e) {
                LOGGER.warn("SpringerLink API request failed", e);
            }
        }
        return pdfLink;
    }
}
