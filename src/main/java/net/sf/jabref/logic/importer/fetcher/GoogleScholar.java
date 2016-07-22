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

package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.importer.FullTextFinder;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * FullTextFinder implementation that attempts to find a PDF URL at GoogleScholar.
 */
public class GoogleScholar implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(GoogleScholar.class);

    private static final String SEARCH_URL = "https://scholar.google.com//scholar?as_q=&as_epq=%s&as_occt=title";
    private static final int NUM_RESULTS = 10;

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Search in title
        if (!entry.hasField(FieldName.TITLE)) {
            return pdfLink;
        }

        String url = String.format(SEARCH_URL,
                URLEncoder.encode(entry.getFieldOptional(FieldName.TITLE).orElse(null), StandardCharsets.UTF_8.name()));

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0") // don't identify as a crawler
                .get();
        // Check results for PDF link
        // TODO: link always on first result or none?
        for (int i = 0; i < NUM_RESULTS; i++) {
            Elements link = doc.select(String.format("#gs_ggsW%s a", i));

            if (link.first() != null) {
                String s = link.first().attr("href");
                // link present?
                if (!"".equals(s)) {
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
