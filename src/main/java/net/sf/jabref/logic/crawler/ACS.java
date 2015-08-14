/*  Copyright (C) 2014 Commonwealth Scientific and Industrial Research Organisation
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
import net.sf.jabref.util.DOI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL from a ACS DOI.
 */
public class ACS implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(ACS.class);

    private static final String SOURCE = "http://pubs.acs.org/doi/abs/%s";

    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     *
     * Currently only uses the DOI if found.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws java.io.IOException
     */
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // DOI search
        Optional<DOI> doi = DOI.build(entry.getField("doi"));

        if(doi.isPresent()) {
            String source = String.format(SOURCE, doi.get().getDOI());
            // Retrieve PDF link
            Document html = Jsoup.connect(source).get();
            Element link = html.select(".pdf-high-res a").first();

            if(link != null) {
                LOGGER.info("Fulltext PDF found @ ACS.");
                pdfLink = Optional.of(new URL(source.replaceFirst("/abs/", "/pdf/")));
            }
        }
        return pdfLink;
    }
}
