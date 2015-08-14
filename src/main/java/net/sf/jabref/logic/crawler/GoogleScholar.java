/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.logic.crawler;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.external.FullTextFinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL from a GoogleScholar search page.
 */
public class GoogleScholar implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(GoogleScholar.class);

    private static final String SEARCH_URL = "https://scholar.google.com//scholar?as_q=&as_epq=%s&as_occt=title";
    private static final int NUM_RESULTS = 10;

    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Search in title
        String entryTitle = entry.getField("title");

        if (entryTitle == null) {
            return pdfLink;
        }

        String url = String.format(SEARCH_URL, URLEncoder.encode(entryTitle, "UTF-8"));

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla") // don't identify as a crawler
                .get();
        // Check results for PDF link
        // TODO: link always on first result or none?
        for (int i = 0; i < NUM_RESULTS; i++) {
            Elements link = doc.select(String.format("#gs_ggsW%s a", i));

            if (link.first() != null) {
                String s = link.first().attr("href");
                // link present?
                if (!s.equals("")) {
                    // TODO: check title inside pdf + length?
                    // TODO: report error function needed?! query -> result
                    LOGGER.info("Fulltext PDF found @ Google: " + s);
                    pdfLink = Optional.of(new URL(s));
                    break;
                }
            }
        }

        return pdfLink;
    }
}
